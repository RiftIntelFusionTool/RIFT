package dev.nohus.rift.jabber.client

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.jabber.client.RosterUsersController.RosterUser
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.MessageBuilder
import org.jivesoftware.smackx.delay.DelayInformationManager
import org.jxmpp.jid.EntityBareJid
import org.koin.core.annotation.Factory
import java.time.Instant
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

@Factory
class UserChatController(
    private val alertsTriggerController: AlertsTriggerController,
) {

    data class ChatsState(
        val chats: List<UserChat> = emptyList(),
        val messages: Map<Chat, List<UserMessage>> = emptyMap(),
    )

    data class UserChat(
        val chat: Chat,
        val name: String,
    )

    data class UserMessage(
        val text: String,
        val isOutgoing: Boolean,
        val timestamp: Instant,
    )

    private val _state = MutableStateFlow(ChatsState())
    val state = _state.asStateFlow()

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val scope = CoroutineScope(Job() + dispatcher)
    private var chatManager: ChatManager? = null
    private var rosterUsersController: RosterUsersController? = null

    fun initialize(connection: XMPPConnection, rosterUsersController: RosterUsersController) {
        this.rosterUsersController = rosterUsersController
        this.chatManager = ChatManager.getInstanceFor(connection).apply {
            addIncomingListener(::onMessageReceived)
            addOutgoingListener(::onMessageSent)
        }
    }

    fun onLogout() {
        chatManager = null
        rosterUsersController = null
        _state.update { ChatsState() }
    }

    suspend fun openConversation(rosterUser: RosterUser): UserChat? = withContext(Dispatchers.IO) {
        val entityBareJid = rosterUser.jid.asEntityBareJidIfPossible() ?: return@withContext null
        val existingChat = _state.value.chats.firstOrNull { it.chat.xmppAddressOfChatPartner == entityBareJid }
        if (existingChat != null) return@withContext existingChat
        chatManager?.chatWith(entityBareJid)?.let { chat ->
            val userChat = UserChat(chat, rosterUser.name)
            _state.update { it.copy(chats = it.chats + userChat) }
            return@withContext userChat
        }
        null
    }

    private fun openConversation(chat: Chat) {
        scope.launch(Dispatchers.IO) {
            val entityBareJid = chat.xmppAddressOfChatPartner ?: return@launch
            if (_state.value.chats.any { it.chat.xmppAddressOfChatPartner == entityBareJid }) return@launch // Chat already open
            val rosterUser = rosterUsersController?.state?.value?.get(entityBareJid.asBareJid())
            val name = rosterUser?.name ?: chat.xmppAddressOfChatPartner.localpartOrNull?.toString() ?: ""
            val userChat = UserChat(chat, name)
            _state.update { it.copy(chats = it.chats + userChat) }
        }
    }

    fun closeConversation(chat: Chat) {
        _state.update { it.copy(chats = it.chats.filter { it.chat != chat }) }
    }

    fun sendMessage(chat: Chat, message: String) {
        try {
            chat.send(message)
        } catch (e: NotConnectedException) {
            logger.error { "Could not send Jabber message, no longer connected" }
        }
    }

    private fun onMessageReceived(from: EntityBareJid, message: Message, chat: Chat) {
        logger.info { "Chat message from $from in ${chat.xmppAddressOfChatPartner}" }
        val sender = chat.xmppAddressOfChatPartner.localpartOrNull?.toString()
        if (sender != "directorbot") {
            openConversation(chat)
        }

        alertsTriggerController.onNewJabberMessage(
            chat = chat.xmppAddressOfChatPartner.localpartOrNull?.toString() ?: "",
            sender = sender ?: "",
            message = message.body,
        )

        val timestamp = DelayInformationManager.getDelayTimestamp(message)?.toInstant() ?: Instant.now()
        val userMessage = UserMessage(
            text = message.body,
            isOutgoing = false,
            timestamp = timestamp,
        )
        val messages = (_state.value.messages[chat] ?: emptyList()) + userMessage
        _state.update { it.copy(messages = it.messages + (chat to messages)) }
    }

    private fun onMessageSent(to: EntityBareJid, messageBuilder: MessageBuilder, chat: Chat) {
        logger.info { "Sent message to $to in ${chat.xmppAddressOfChatPartner}" }
        val userMessage = UserMessage(
            text = messageBuilder.body,
            isOutgoing = true,
            timestamp = Instant.now(),
        )
        val messages = (_state.value.messages[chat] ?: emptyList()) + userMessage
        _state.update { it.copy(messages = it.messages + (chat to messages)) }
    }
}
