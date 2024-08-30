package dev.nohus.rift.intel.reports.settings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class IntelReportsSettingsViewModel(
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val isUsingCompactMode: Boolean,
        val isShowingReporter: Boolean,
        val isShowingChannel: Boolean,
        val isShowingRegion: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            isUsingCompactMode = settings.intelReports.isUsingCompactMode,
            isShowingReporter = settings.intelReports.isShowingReporter,
            isShowingChannel = settings.intelReports.isShowingChannel,
            isShowingRegion = settings.intelReports.isShowingRegion,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        isUsingCompactMode = settings.intelReports.isUsingCompactMode,
                        isShowingReporter = settings.intelReports.isShowingReporter,
                        isShowingChannel = settings.intelReports.isShowingChannel,
                        isShowingRegion = settings.intelReports.isShowingRegion,
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
}
