package dev.nohus.rift.jabber

import dev.nohus.rift.ViewModel
import dev.nohus.rift.jabber.JabberAccountRepository.JabberAccountResult
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.jabber.client.JabberClient.LoginResult
import dev.nohus.rift.jabber.client.MultiUserChatController
import dev.nohus.rift.jabber.client.RosterUsersController.RosterUser
import dev.nohus.rift.jabber.client.StartJabberUseCase
import dev.nohus.rift.jabber.client.UserChatController
import dev.nohus.rift.jabber.client.UserChatController.UserChat
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jxmpp.jid.EntityBareJid
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Factory
class JabberViewModel(
    @InjectedParam private val inputModel: JabberInputModel,
    private val jabberAccountRepository: JabberAccountRepository,
    private val detectJabberAccountUseCase: DetectJabberAccountUseCase,
    private val jabberClient: JabberClient,
    private val startJabberUseCase: StartJabberUseCase,
    private val settings: Settings,
) : ViewModel() {

    sealed class UiState(val contentKey: Int) {
        data object Connecting : UiState(1)
        data class NoAccount(
            val canImport: Boolean,
        ) : UiState(3)
        data class Login(
            val errorMessage: String?,
        ) : UiState(4)
        data class LoggedIn(
            val jabberState: JabberClient.JabberState,
            val contactListState: ContactListState = ContactListState.Contacts,
            val collapsedGroups: List<String>,
            val selectedTab: TabModel = TabModel.Contacts,
            val unreadChats: List<EntityBareJid> = emptyList(),
        ) : UiState(5)
    }

    sealed interface ContactListState {
        data object Contacts : ContactListState
        data class AddContact(
            val error: String? = null,
        ) : ContactListState
        data class AddChatRoom(
            val error: String? = null,
        ) : ContactListState
    }

    sealed interface TabModel {
        data object Contacts : TabModel
        data class UserChat(val chat: UserChatController.UserChat) : TabModel
        data class MultiUserChat(val chat: EntityBareJid) : TabModel
    }

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Connecting)
    val state = _state.asStateFlow()

    private var lastReadPerChat: MutableMap<EntityBareJid, Instant> = mutableMapOf()

    init {
        viewModelScope.launch {
            jabberClient.state.collect { jabberState ->
                val state = _state.value
                if (state is UiState.Connecting && jabberState.isConnected) {
                    setLoggedInState(jabberState)
                }
                if (state is UiState.LoggedIn) {
                    _state.update {
                        state.copy(
                            jabberState = jabberState,
                            unreadChats = getUnreadChats(jabberState.userChatMessages, jabberState.multiUserChatMessages),
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            settings.updateFlow.map { it.jabberCollapsedGroups }.collect { groups ->
                _state.update { if (it is UiState.LoggedIn) it.copy(collapsedGroups = groups) else it }
            }
        }
        when (jabberAccountRepository.getAccount()) {
            JabberAccountResult.NoAccount -> { _state.update { UiState.NoAccount(canImport = detectJabberAccountUseCase() != null) } }
            is JabberAccountResult.JabberAccount -> if (jabberClient.state.value.isConnected) {
                setLoggedInState(jabberClient.state.value)
            } else {
                _state.update { UiState.Connecting }
            }
        }
    }

    private fun setLoggedInState(jabberState: JabberClient.JabberState) {
        _state.update { UiState.LoggedIn(jabberState = jabberState, collapsedGroups = settings.jabberCollapsedGroups) }
    }

    fun onLogoutClick() {
        jabberClient.logout()
        jabberAccountRepository.clearAccount()
        _state.update { UiState.NoAccount(canImport = detectJabberAccountUseCase() != null) }
    }

    fun onTabSelect(model: TabModel) {
        updateSelectedTab(model)
    }

    fun onImportClick() {
        val account = detectJabberAccountUseCase() ?: return
        connect(account.jidLocalPart, account.password)
    }

    fun onLoginClick() {
        _state.update { UiState.Login(errorMessage = null) }
    }

    fun onConnectClick(jidLocalPart: String, password: String) {
        if (jidLocalPart.isNotBlank() && password.isNotBlank()) {
            connect(jidLocalPart, password)
        } else {
            _state.update { UiState.Login(errorMessage = "You need to enter a username and password") }
        }
    }

    fun onRosterUserClick(rosterUser: RosterUser) {
        viewModelScope.launch {
            val chat = jabberClient.openChat(rosterUser)
            if (chat != null) {
                updateSelectedTab(TabModel.UserChat(chat))
            }
        }
    }

    fun onRosterUserRemove(rosterUser: RosterUser) {
        viewModelScope.launch {
            jabberClient.removeContact(rosterUser)
        }
    }

    fun onChatClosed(chat: Chat) {
        jabberClient.closeChat(chat)
        updateSelectedTab(TabModel.Contacts)
    }

    fun onChatRoomClick(multiUserChat: MultiUserChat) {
        jabberClient.openChatRoom(multiUserChat)
        updateSelectedTab(TabModel.MultiUserChat(multiUserChat.room))
    }

    fun onChatRoomRemove(multiUserChat: MultiUserChat) {
        jabberClient.removeChatRoom(multiUserChat)
    }

    fun onChatRoomClosed(multiUserChat: MultiUserChat) {
        jabberClient.closeChatRoom(multiUserChat)
        updateSelectedTab(TabModel.Contacts)
    }

    fun onAddContactClick() {
        updateLoggedInState { copy(contactListState = ContactListState.AddContact()) }
    }

    fun onAddChatRoomClick() {
        updateLoggedInState { copy(contactListState = ContactListState.AddChatRoom()) }
    }

    fun onAddContactSubmitClick(jidLocalPart: String, name: String, groups: List<String>) {
        val state = _state.value
        if (state is UiState.LoggedIn) {
            val contactListState = state.contactListState
            if (contactListState is ContactListState.AddContact) {
                if (jidLocalPart.isBlank()) {
                    _state.update { state.copy(contactListState = contactListState.copy(error = "Enter the Jabber ID")) }
                } else if (name.isBlank()) {
                    _state.update { state.copy(contactListState = contactListState.copy(error = "Enter the nickname")) }
                } else {
                    viewModelScope.launch {
                        jabberClient.addContact(jidLocalPart, name, groups)
                    }
                    _state.update { state.copy(contactListState = ContactListState.Contacts) }
                }
            }
        }
    }

    fun onAddChatRoomSubmitClick(jidLocalPart: String) {
        val state = _state.value
        if (state is UiState.LoggedIn) {
            val contactListState = state.contactListState
            if (contactListState is ContactListState.AddChatRoom) {
                if (jidLocalPart.isBlank()) {
                    _state.update { state.copy(contactListState = contactListState.copy(error = "Enter the room name")) }
                } else {
                    viewModelScope.launch {
                        jabberClient.addChatRoom(jidLocalPart)
                    }
                    _state.update { state.copy(contactListState = ContactListState.Contacts) }
                }
            }
        }
    }

    fun onBackClick() {
        updateLoggedInState { copy(contactListState = ContactListState.Contacts) }
    }

    fun onMessageSend(userChat: UserChat, message: String) {
        if (message.isNotBlank()) {
            viewModelScope.launch {
                jabberClient.sendMessage(userChat.chat, message)
            }
        }
    }

    fun onMessageSend(multiUserChat: MultiUserChat, message: String) {
        if (message.isNotBlank()) {
            viewModelScope.launch {
                jabberClient.sendMessage(multiUserChat, message)
            }
        }
    }

    fun onCollapsedGroupToggle(group: String) {
        if (group in settings.jabberCollapsedGroups) {
            settings.jabberCollapsedGroups -= group
        } else {
            settings.jabberCollapsedGroups += group
        }
    }

    private fun connect(jidLocalPart: String, password: String) {
        jabberAccountRepository.setAccount(jidLocalPart, password)
        _state.update { UiState.Connecting }
        connect()
    }

    private fun connect() = viewModelScope.launch {
        val result = startJabberUseCase()
        when (result) {
            LoginResult.Success -> {
                setLoggedInState(jabberClient.state.value)
            }
            LoginResult.IncorrectPassword -> {
                _state.update { UiState.Login(errorMessage = "Couldn't connect because your password is incorrect") }
                jabberAccountRepository.clearAccount()
            }
            LoginResult.AuthenticationFailure -> {
                _state.update { UiState.Login(errorMessage = "Couldn't connect due to an authentication failure") }
            }
            LoginResult.ConnectionFailure -> {
                _state.update { UiState.Login(errorMessage = "Couldn't connect to the server") }
            }
            is LoginResult.Error -> {
                _state.update { UiState.Login(errorMessage = "Couldn't connect") }
            }
            null -> { // No account
                _state.update { UiState.Login(errorMessage = null) }
            }
        }
    }

    private fun getUnreadChats(
        userChats: Map<Chat, List<UserChatController.UserMessage>>,
        multiUserChats: Map<MultiUserChat, List<MultiUserChatController.MultiUserMessage>>,
    ): List<EntityBareJid> {
        val unreadUserChats = userChats.entries.filter {
            val lastMessageTimestamp = it.value.lastOrNull()?.timestamp ?: return@filter false
            val lastReadTimestamp = lastReadPerChat[it.key.xmppAddressOfChatPartner] ?: return@filter true
            lastMessageTimestamp > lastReadTimestamp
        }.map { it.key.xmppAddressOfChatPartner }
        val unreadMultiUserChats = multiUserChats.entries.filter {
            val lastMessageTimestamp = it.value.lastOrNull()?.timestamp ?: return@filter false
            val lastReadTimestamp = lastReadPerChat[it.key.room] ?: return@filter true
            lastMessageTimestamp > lastReadTimestamp
        }.map { it.key.room }
        return unreadUserChats + unreadMultiUserChats
    }

    private fun updateSelectedTab(tab: TabModel) {
        val currentTab = (_state.value as? UiState.LoggedIn)?.selectedTab ?: return
        updateLastRead(currentTab)
        updateLastRead(tab)
        updateLoggedInState {
            copy(
                selectedTab = tab,
                unreadChats = getUnreadChats(jabberState.userChatMessages, jabberState.multiUserChatMessages),
            )
        }
    }

    private fun updateLastRead(tab: TabModel) {
        when (tab) {
            TabModel.Contacts -> {}
            is TabModel.MultiUserChat -> lastReadPerChat[tab.chat] = Instant.now()
            is TabModel.UserChat -> lastReadPerChat[tab.chat.chat.xmppAddressOfChatPartner] = Instant.now()
        }
    }

    private fun updateLoggedInState(update: UiState.LoggedIn.() -> UiState.LoggedIn) {
        val state = _state.value
        if (state is UiState.LoggedIn) {
            _state.update { update(state) }
        }
    }
}
