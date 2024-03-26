package dev.nohus.rift.intel.settings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class IntelSettingsViewModel(
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val isUsingCompact: Boolean,
        val isShowingReporter: Boolean,
        val isShowingChannel: Boolean,
        val isShowingRegion: Boolean,
        val isShowingSystemDistance: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            isUsingCompact = settings.intelReports.isUsingCompactMode,
            isShowingReporter = settings.intelReports.isShowingReporter,
            isShowingChannel = settings.intelReports.isShowingChannel,
            isShowingRegion = settings.intelReports.isShowingRegion,
            isShowingSystemDistance = settings.intelReports.isShowingSystemDistance,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        isUsingCompact = settings.intelReports.isUsingCompactMode,
                        isShowingReporter = settings.intelReports.isShowingReporter,
                        isShowingChannel = settings.intelReports.isShowingChannel,
                        isShowingRegion = settings.intelReports.isShowingRegion,
                        isShowingSystemDistance = settings.intelReports.isShowingSystemDistance,
                    )
                }
            }
        }
    }

    fun onIsUsingCompactModeChange(enabled: Boolean) {
        settings.intelReports = settings.intelReports.copy(isUsingCompactMode = enabled)
    }

    fun onIsShowingReporterChange(enabled: Boolean) {
        settings.intelReports = settings.intelReports.copy(isShowingReporter = enabled)
    }

    fun onIsShowingChannelChange(enabled: Boolean) {
        settings.intelReports = settings.intelReports.copy(isShowingChannel = enabled)
    }

    fun onIsShowingRegionChange(enabled: Boolean) {
        settings.intelReports = settings.intelReports.copy(isShowingRegion = enabled)
    }

    fun onIsShowingSystemDistanceChange(enabled: Boolean) {
        settings.intelReports = settings.intelReports.copy(isShowingSystemDistance = enabled)
    }
}
