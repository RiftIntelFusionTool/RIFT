package dev.nohus.rift.debug

import ch.qos.logback.classic.spi.ILoggingEvent
import dev.nohus.rift.ViewModel
import dev.nohus.rift.about.GetVersionUseCase
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.logging.LoggingRepository
import dev.nohus.rift.network.killboard.KillboardObserver
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.OperatingSystem
import kotlinx.coroutines.delay
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
    private val killboardObserver: KillboardObserver,
    private val jabberClient: JabberClient,
    operatingSystem: OperatingSystem,
) : ViewModel() {

    data class UiState(
        val events: List<ILoggingEvent> = emptyList(),
        val displayTimezone: ZoneId,
        val version: String,
        val vmVersion: String,
        val operatingSystem: OperatingSystem,
        val isZkillboardConnected: Boolean,
        val isEveKillConnected: Boolean,
        val isJabberConnected: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            displayTimezone = settings.displayTimeZone,
            version = getVersionUseCase(),
            vmVersion = getVmVersion(),
            operatingSystem = operatingSystem,
            isZkillboardConnected = killboardObserver.isZkillboardConnected,
            isEveKillConnected = killboardObserver.isEveKillConnected,
            isJabberConnected = jabberClient.state.value.isConnected,
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
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update {
                    it.copy(
                        isZkillboardConnected = killboardObserver.isZkillboardConnected,
                        isEveKillConnected = killboardObserver.isEveKillConnected,
                        isJabberConnected = jabberClient.state.value.isConnected,
                    )
                }
            }
        }
    }

    private fun getVmVersion(): String {
        return "${System.getProperty("java.vm.vendor")} ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}"
    }
}
