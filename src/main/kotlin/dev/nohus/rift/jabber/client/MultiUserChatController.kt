package dev.nohus.rift.jabber.client

import dev.nohus.rift.alerts.AlertsTriggerController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Message.Subject
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.delay.DelayInformationManager
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import org.jxmpp.stringprep.XmppStringprepException
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

@Factory
class MultiUserChatController(
    private val alertsTriggerController: AlertsTriggerController,
) {

    data class ChatsState(
        val chats: List<MultiUserChat> = emptyList(),
        val openChats: List<EntityBareJid> = emptyList(),
        val subjects: Map<EntityBareJid, String> = emptyMap(),
        val messages: Map<MultiUserChat, List<MultiUserMessage>> = emptyMap(),
    )

    data class MultiUserMessage(
        val text: String,
        val sender: String?, // null for outgoing messages
        val timestamp: Instant,
    )

    private val _state = MutableStateFlow(ChatsState())
    val state = _state.asStateFlow()

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val scope = CoroutineScope(Job() + dispatcher)
    private var nickname = ""
    private var bookmarkManager: BookmarkManager? = null
    private var multiUserChatManager: MultiUserChatManager? = null

    fun initialize(connection: XMPPConnection, jid: String) {
        this.nickname = JidCreate.entityBareFrom(jid).localpart.toString()
        this.bookmarkManager = BookmarkManager.getBookmarkManager(connection)
        this.multiUserChatManager = MultiUserChatManager.getInstanceFor(connection)
    }

    fun onLogout() {
        nickname = ""
        bookmarkManager = null
        multiUserChatManager = null
        _state.update { ChatsState() }
    }

    fun openChat(multiUserChat: MultiUserChat) {
        if (multiUserChat.room !in _state.value.openChats) {
            _state.update { it.copy(openChats = it.openChats + multiUserChat.room) }
        }
    }

    fun closeChat(multiUserChat: MultiUserChat) {
        _state.update { it.copy(openChats = it.openChats - multiUserChat.room) }
    }

    /**
     * Adds to server-side bookmarked chats
     */
    fun addChatRoom(jidLocalPart: String) {
        try {
            val entityBareJid = JidCreate.entityBareFrom("$jidLocalPart@conference.goonfleet.com")
            val name = entityBareJid.localpart.toString()
            bookmarkManager?.addBookmarkedConference(name, entityBareJid, true, Resourcepart.from(nickname), null)
            multiUserChatManager?.let { multiUserChatManager ->
                joinChat(multiUserChatManager, entityBareJid)
            }
        } catch (e: SmackException) {
            logger.error { "Could not add chat room: $e" }
        } catch (e: XMPPException) {
            logger.error { "Could not add chat room: $e" }
        } catch (e: XmppStringprepException) {
            logger.error { "Could not add chat room, invalid JID: $e" }
        }
    }

    /**
     * Removes from server-side bookmarked chats
     */
    fun removeChatRoom(jid: EntityBareJid) {
        bookmarkManager?.removeBookmarkedConference(jid)
        _state.update {
            it.copy(
                chats = it.chats.filter { it.room != jid },
                openChats = it.openChats.filter { it != jid },
            )
        }
    }

    fun joinBookmarkedChats() {
        val bookmarkManager = this.bookmarkManager ?: return
        val multiUserChatManager = this.multiUserChatManager ?: return
        bookmarkManager.bookmarkedConferences.forEach {
            if (it.isAutoJoin) {
                joinChat(multiUserChatManager, it.jid)
            }
        }
    }

    fun sendMessage(chat: MultiUserChat, message: String) {
        chat.sendMessage(message)
    }

    private fun joinChat(muc: MultiUserChatManager, jid: EntityBareJid) {
        val chat = muc.getMultiUserChat(jid)
        chat.addMessageListener(::onChatMessage)
        scope.launch {
            try {
                chat.join(Resourcepart.from(nickname))
                _state.update { it.copy(chats = it.chats + chat) }
            } catch (e: XMPPException) {
                logger.error { "Could not join chat: $e" }
            } catch (e: SmackException) {
                logger.error { "Could not join chat: $e" }
            }
        }
    }

    private fun onChatMessage(message: Message) {
        val chat = _state.value.chats.firstOrNull { it.room == message.from.asEntityBareJidIfPossible() } ?: return

        message.extensions.filterIsInstance<Subject>().firstOrNull()?.let { extension ->
            _state.update { it.copy(subjects = it.subjects + (chat.room to extension.subject)) }
        }

        val sender = message.from.resourceOrNull ?: return
        val body = message.body ?: return
        val timestamp = DelayInformationManager.getDelayTimestamp(message)?.toInstant() ?: Instant.now()

        if (Duration.between(timestamp, Instant.now()) < Duration.ofMinutes(1)) {
            alertsTriggerController.onNewJabberMessage(
                chat = chat.room.localpartOrNull?.toString() ?: "",
                sender = sender.toString(),
                message = body,
            )
        }

        val messages = _state.value.messages[chat] ?: emptyList()
        val newMessages = messages + MultiUserMessage(
            text = body,
            sender = sender.toString(),
            timestamp = timestamp,
        )
        _state.update { it.copy(messages = it.messages + (chat to newMessages)) }
    }
}
