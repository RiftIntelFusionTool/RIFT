package dev.nohus.rift.intel.feed.settings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class IntelFeedSettingsViewModel(
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val isUsingCompactMode: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            isUsingCompactMode = settings.intelFeed.isUsingCompactMode,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        isUsingCompactMode = settings.intelFeed.isUsingCompactMode,
                    )
                }
            }
        }
    }

    fun onIsUsingCompactModeChange(enabled: Boolean) {
        settings.intelFeed = settings.intelFeed.copy(isUsingCompactMode = enabled)
    }
}
