package dev.nohus.rift.map.settings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.IntelMap
import dev.nohus.rift.settings.persistence.MapStarColor
import dev.nohus.rift.settings.persistence.MapType
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class MapSettingsViewModel(
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val intelMap: IntelMap,
    )

    private val _state = MutableStateFlow(
        UiState(intelMap = settings.intelMap),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(intelMap = settings.intelMap)
                }
            }
        }
    }

    fun onStarColorChange(mapType: MapType, selected: MapStarColor) {
        val new = settings.intelMap.mapTypeStarColor + (mapType to selected)
        settings.intelMap = settings.intelMap.copy(mapTypeStarColor = new)
    }

    fun onIntelExpireSecondsChange(seconds: Int) {
        settings.intelMap = settings.intelMap.copy(intelExpireSeconds = seconds)
    }

    fun onIntelPopupTimeoutSecondsChange(seconds: Int) {
        settings.intelMap = settings.intelMap.copy(intelPopupTimeoutSeconds = seconds)
    }

    fun onIsUsingCompactModeChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isUsingCompactMode = enabled)
    }

    fun onIsCharacterFollowingChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isCharacterFollowing = enabled)
    }

    fun onIsScrollZoomInvertedChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isInvertZoom = enabled)
    }
}
