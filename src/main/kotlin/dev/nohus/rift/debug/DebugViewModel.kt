package dev.nohus.rift.debug

import ch.qos.logback.classic.spi.ILoggingEvent
import dev.nohus.rift.ViewModel
import dev.nohus.rift.about.GetVersionUseCase
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
    getVersionUseCase: GetVersionUseCase,
) : ViewModel() {

    data class UiState(
        val events: List<ILoggingEvent> = emptyList(),
        val displayTimezone: ZoneId,
        val version: String,
        val vmVersion: String,
    )

    private val _state = MutableStateFlow(
        UiState(
            displayTimezone = settings.displayTimeZone,
            version = getVersionUseCase(),
            vmVersion = getVmVersion(),
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

    private fun getVmVersion(): String {
        return "${System.getProperty("java.vm.vendor")} ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}"
    }
}
