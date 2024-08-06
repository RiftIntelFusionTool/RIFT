package dev.nohus.rift.debug

import ch.qos.logback.classic.spi.ILoggingEvent
import dev.nohus.rift.ViewModel
import dev.nohus.rift.logging.LoggingRepository
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.ZoneId

@Single
class DebugViewModel(
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val events: List<ILoggingEvent> = emptyList(),
        val displayTimezone: ZoneId,
    )

    private val _state = MutableStateFlow(
        UiState(
            displayTimezone = settings.displayTimeZone,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            LoggingRepository.state.collect { logs ->
                _state.update { it.copy(events = logs) }
            }
        }
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        displayTimezone = settings.displayTimeZone,
                    )
                }
            }
        }
    }
}
