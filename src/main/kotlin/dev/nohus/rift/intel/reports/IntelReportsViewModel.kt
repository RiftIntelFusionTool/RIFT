package dev.nohus.rift.intel.reports

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.intel.ChatLogWatcher
import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.reports.settings.IntelReportsSettings
import dev.nohus.rift.intel.state.AlertTriggeringMessagesRepository
import dev.nohus.rift.intel.state.AlertTriggeringMessagesRepository.AlertTriggeringMessage
import dev.nohus.rift.logs.parse.ChatMessageParser
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class IntelReportsViewModel(
    private val logWatcher: ChatLogWatcher,
    private val settings: Settings,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val alertTriggeringMessagesRepository: AlertTriggeringMessagesRepository,
) : ViewModel() {

    data class UiState(
        val intelChannels: List<IntelChannel> = emptyList(),
        val filteredChannel: IntelChannel? = null,
        val search: String? = null,
        val channelChatMessages: List<ParsedChannelChatMessage> = emptyList(),
        val alertTriggeringMessages: List<AlertTriggeringMessage> = emptyList(),
        val hasOnlineCharacters: Boolean = false,
        val settings: IntelReportsSettings,
    )

    private val _state = MutableStateFlow(
        UiState(settings = getSettings()),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(intelChannels = settings.intelChannels) }
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        intelChannels = settings.intelChannels,
                        settings = getSettings(),
                    )
                }
            }
        }
        viewModelScope.launch {
            logWatcher.channelChatMessages.collect { channelChatMessages ->
                _state.update { it.copy(channelChatMessages = getFilteredMessages(channelChatMessages)) }
            }
        }
        viewModelScope.launch {
            onlineCharactersRepository.onlineCharacters.map { it.isNotEmpty() }.collect { hasOnlineCharacters ->
                _state.update { it.copy(hasOnlineCharacters = hasOnlineCharacters) }
            }
        }
        viewModelScope.launch {
            alertTriggeringMessagesRepository.messages.collect { messages ->
                _state.update { it.copy(alertTriggeringMessages = messages) }
            }
        }
    }

    fun onIntelChannelFilterSelect(channelName: String) {
        val channel = _state.value.intelChannels.firstOrNull { it.name == channelName }
        _state.update { it.copy(filteredChannel = channel) }
        updateFilteredMessages()
    }

    fun onSearchChange(text: String) {
        val search = text.takeIf { it.isNotBlank() }?.trim()
        _state.update { it.copy(search = search) }
        updateFilteredMessages()
    }

    private fun updateFilteredMessages() {
        val messages = getFilteredMessages(logWatcher.channelChatMessages.value)
        _state.update { it.copy(channelChatMessages = messages) }
    }

    private fun getSettings(): IntelReportsSettings {
        return IntelReportsSettings(
            displayTimezone = settings.displayTimeZone,
            isUsingCompactMode = settings.intelReports.isUsingCompactMode,
            isShowingReporter = settings.intelReports.isShowingReporter,
            isShowingChannel = settings.intelReports.isShowingChannel,
            isShowingRegion = settings.intelReports.isShowingRegion,
            isShowingSystemDistance = settings.isShowingSystemDistance,
            isUsingJumpBridgesForDistance = settings.isUsingJumpBridgesForDistance,
        )
    }

    private fun getFilteredMessages(messages: List<ParsedChannelChatMessage>): List<ParsedChannelChatMessage> {
        val filteredChannel = _state.value.filteredChannel
        val search = _state.value.search?.lowercase()
        return messages
            .filter { message ->
                val matchesChannelFilter = filteredChannel == null || message.metadata.channelName == filteredChannel.name
                val matchesSearchFilter = search == null || search in message
                matchesChannelFilter && matchesSearchFilter
            }
    }

    private operator fun ParsedChannelChatMessage.contains(term: String): Boolean {
        if (term in chatMessage.message.lowercase()) return true
        return parsed.flatMap { it.types }.any { token ->
            when (token) {
                is ChatMessageParser.TokenType.Count -> false
                is ChatMessageParser.TokenType.Gate -> term in token.system.lowercase()
                is ChatMessageParser.TokenType.Keyword -> term in token.type.name.lowercase()
                is ChatMessageParser.TokenType.Kill -> term in token.name.lowercase() || term in token.target.lowercase()
                ChatMessageParser.TokenType.Link -> false
                is ChatMessageParser.TokenType.Movement -> term in token.toSystem.lowercase() || term in token.verb.lowercase()
                is ChatMessageParser.TokenType.Player -> false
                is ChatMessageParser.TokenType.Question -> term in token.type.name.lowercase()
                is ChatMessageParser.TokenType.Ship -> term in token.name.lowercase()
                is ChatMessageParser.TokenType.System -> term in token.name.lowercase()
                ChatMessageParser.TokenType.Url -> false
            }
        }
    }
}
