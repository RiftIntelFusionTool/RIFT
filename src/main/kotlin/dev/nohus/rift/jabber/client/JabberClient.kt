package dev.nohus.rift.jabber.client

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.jabber.client.MultiUserChatController.MultiUserMessage
import dev.nohus.rift.jabber.client.RosterUsersController.RosterUser
import dev.nohus.rift.jabber.client.UserChatController.UserMessage
import dev.nohus.rift.utils.combineStates
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.sasl.SASLError
import org.jivesoftware.smack.sasl.SASLErrorException
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jivesoftware.smackx.iqversion.VersionManager
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.EntityBareJid
import org.koin.core.annotation.Single
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Single
class JabberClient(
    private val rosterUsersController: RosterUsersController,
    private val multiUserChatController: MultiUserChatController,
    private val userChatController: UserChatController,
) {

    data class JabberState(
        val isConnected: Boolean = false,
        val users: Map<BareJid, RosterUser>,
        val multiUserChats: List<MultiUserChat>,
        val openMultiUserChats: List<EntityBareJid>,
        val multiUserChatSubjects: Map<EntityBareJid, String>,
        val multiUserChatMessages: Map<MultiUserChat, List<MultiUserMessage>>,
        val userChats: List<UserChatController.UserChat>,
        val userChatMessages: Map<Chat, List<UserMessage>>,
    )

    private data class JabberClientState(
        val isConnected: Boolean = false,
    )

    private val _state = MutableStateFlow(JabberClientState())
    val state = combineStates(
        _state,
        rosterUsersController.state,
        multiUserChatController.state,
        userChatController.state,
    ) { state, users, multiUserChats, userChats ->
        JabberState(
            isConnected = state.isConnected,
            users = users,
            multiUserChats = multiUserChats.chats,
            openMultiUserChats = multiUserChats.openChats,
            multiUserChatSubjects = multiUserChats.subjects,
            multiUserChatMessages = multiUserChats.messages,
            userChats = userChats.chats,
            userChatMessages = userChats.messages,
        )
    }

    private data class JabberSession(
        var connection: XMPPTCPConnection,
        val chatStateManager: ChatStateManager,
    )

    private val loginMutex = Mutex()
    private var session: JabberSession? = null

    sealed interface LoginResult {
        data object Success : LoginResult
        data object IncorrectPassword : LoginResult
        data object AuthenticationFailure : LoginResult
        data object ConnectionFailure : LoginResult
        data class Error(val cause: Exception) : LoginResult
    }

    suspend fun login(jid: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        loginMutex.withLock {
            try {
                logout()
                val configuration = XMPPTCPConnectionConfiguration.builder()
                    .setXmppAddressAndPassword(jid, password)
                    .setResource("RIFT")
                    .build()
                XMPPTCPConnection(configuration).apply {
                    val reconnectionManager = ReconnectionManager.getInstanceFor(this)
                    reconnectionManager.enableAutomaticReconnection()
                    setParsingExceptionCallback {
                        logger.error { "Parsing exception. Content: \"${it.content}\", Exception: ${it.parsingException}" }
                    }
                    addConnectionListener(object : ConnectionListener {
                        override fun connectionClosedOnError(e: Exception) {
                            onConnectionClosed(e)
                        }
                    })
                    addStanzaListener(::onStanzaReceived) { true }
                    rosterUsersController.initialize(this)
                    val chatStateManager = ChatStateManager.getInstance(this).apply {
                        addChatStateListener(::onChatStateChanged)
                    }
                    VersionManager.setAutoAppendSmackVersion(false)
                    VersionManager.getInstanceFor(this).apply {
                        setVersion("RIFT Intel Fusion Tool", BuildConfig.version)
                    }
                    multiUserChatController.initialize(this, jid)
                    userChatController.initialize(this, rosterUsersController)

                    connect()
                    login()

                    multiUserChatController.joinBookmarkedChats()

                    session = JabberSession(
                        connection = this,
                        chatStateManager = chatStateManager,
                    )
                    _state.update { it.copy(isConnected = true) }
                }
                logger.info { "Logged in to Jabber" }
                LoginResult.Success
            } catch (e: Exception) {
                if (e is SASLErrorException) {
                    if (e.saslFailure.saslError == SASLError.not_authorized) {
                        LoginResult.IncorrectPassword
                    } else {
                        LoginResult.AuthenticationFailure
                    }
                } else if (e is IOException) {
                    logger.error(e) { "Jabber login I/O error" }
                    LoginResult.ConnectionFailure
                } else {
                    logger.error(e) { "Jabber login error" }
                    LoginResult.Error(e)
                }
            }
        }
    }

    fun logout() {
        _state.update { JabberClientState(isConnected = false) }
        session?.connection?.disconnect()
        session = null
        rosterUsersController.onLogout()
        userChatController.onLogout()
        multiUserChatController.onLogout()
    }

    suspend fun openChat(rosterUser: RosterUser): UserChatController.UserChat? {
        return userChatController.openConversation(rosterUser)
    }

    fun closeChat(chat: Chat) {
        userChatController.closeConversation(chat)
    }

    fun openChatRoom(multiUserChat: MultiUserChat) {
        multiUserChatController.openChat(multiUserChat)
    }

    fun closeChatRoom(multiUserChat: MultiUserChat) {
        multiUserChatController.closeChat(multiUserChat)
    }

    suspend fun addContact(jidLocalPart: String, name: String, groups: List<String>) = withContext(Dispatchers.IO) {
        rosterUsersController.addContact(jidLocalPart, name, groups)
    }

    suspend fun addChatRoom(jidLocalPart: String) = withContext(Dispatchers.IO) {
        multiUserChatController.addChatRoom(jidLocalPart)
    }

    suspend fun removeContact(rosterUser: RosterUser) = withContext(Dispatchers.IO) {
        rosterUsersController.removeContact(rosterUser)
    }

    fun removeChatRoom(multiUserChat: MultiUserChat) {
        multiUserChatController.removeChatRoom(multiUserChat.room)
    }

    suspend fun sendMessage(chat: Chat, message: String) = withContext(Dispatchers.IO) {
        userChatController.sendMessage(chat, message)
    }

    suspend fun sendMessage(multiUserChat: MultiUserChat, message: String) = withContext(Dispatchers.IO) {
        multiUserChatController.sendMessage(multiUserChat, message)
    }

    private fun onStanzaReceived(stanza: Stanza) {
        when (stanza) {
            is Presence -> {} // Handled by Roster class
            is Message -> {} // Handled by message listeners
            is IQ -> onIqStanzaReceived(stanza)
        }
    }

    private fun onIqStanzaReceived(iq: IQ) {
        when (iq.type ?: return) {
            IQ.Type.get -> logger.info { "IQ Get Stanza received: $iq" }
            IQ.Type.set -> logger.info { "IQ Set Stanza received: $iq" }
            IQ.Type.result -> {}
            IQ.Type.error -> logger.info { "IQ Error Stanza received: $iq, ${iq.childElementXML}" }
        }
    }

    private fun onChatStateChanged(chat: Chat, state: ChatState, message: Message) {
        logger.info { "Chat state of ${chat.xmppAddressOfChatPartner}: ${state.name}." }
    }

    private fun onConnectionClosed(e: Exception) {
        logger.error { "Jabber connection closed: $e" }
    }
}
