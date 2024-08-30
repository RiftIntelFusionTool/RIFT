package dev.nohus.rift.map.settings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.map.settings.JumpBridgesParser.JumpBridgeNetwork
import dev.nohus.rift.repositories.JumpBridgesRepository
import dev.nohus.rift.settings.persistence.IntelMap
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class MapSettingsViewModel(
    private val settings: Settings,
    private val clipboard: Clipboard,
    private val jumpBridgesParser: JumpBridgesParser,
    configurationPackRepository: ConfigurationPackRepository,
    private val jumpBridgesRepository: JumpBridgesRepository,
) : ViewModel() {

    data class UiState(
        val intelMap: IntelMap,
        val isUsingRiftAutopilotRoute: Boolean,
        val jumpBridgeNetworkState: JumpBridgeNetworkState,
        val jumpBridgeCopyState: JumpBridgeCopyState,
        val jumpBridgeNetworkUrl: String?,
        val jumpBridgeSearchState: JumpBridgeSearchState,
        val isJumpBridgeSearchDialogShown: Boolean,
    )

    sealed interface JumpBridgeNetworkState {
        data object Empty : JumpBridgeNetworkState
        data class Loaded(val network: JumpBridgeNetwork) : JumpBridgeNetworkState
    }

    sealed interface JumpBridgeCopyState {
        data object NotCopied : JumpBridgeCopyState
        data class Copied(val network: JumpBridgeNetwork) : JumpBridgeCopyState
    }

    sealed interface JumpBridgeSearchState {
        data object NotSearched : JumpBridgeSearchState
        data class Searching(val progress: Float, val connectionsCount: Int) : JumpBridgeSearchState
        data object SearchFailed : JumpBridgeSearchState
        data class SearchDone(val network: JumpBridgeNetwork) : JumpBridgeSearchState
    }

    private val _state = MutableStateFlow(
        UiState(
            intelMap = settings.intelMap,
            isUsingRiftAutopilotRoute = settings.isUsingRiftAutopilotRoute,
            jumpBridgeNetworkState = jumpBridgesRepository.getConnections()?.let {
                JumpBridgeNetworkState.Loaded(JumpBridgeNetwork(it))
            } ?: JumpBridgeNetworkState.Empty,
            jumpBridgeCopyState = JumpBridgeCopyState.NotCopied,
            jumpBridgeNetworkUrl = configurationPackRepository.getJumpBridgeNetworkUrl(),
            jumpBridgeSearchState = JumpBridgeSearchState.NotSearched,
            isJumpBridgeSearchDialogShown = false,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        intelMap = settings.intelMap,
                        isUsingRiftAutopilotRoute = settings.isUsingRiftAutopilotRoute,
                    )
                }
            }
        }
        viewModelScope.launch {
            clipboard.state.filterNotNull().collect { text ->
                val network = jumpBridgesParser.parse(text)
                if (network != null) {
                    _state.update { it.copy(jumpBridgeCopyState = JumpBridgeCopyState.Copied(network)) }
                } else {
                    _state.update { it.copy(jumpBridgeCopyState = JumpBridgeCopyState.NotCopied) }
                }
            }
        }
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

    fun onIsUsingRiftAutopilotRouteChange(enabled: Boolean) {
        settings.isUsingRiftAutopilotRoute = enabled
    }

    fun onJumpBridgeForgetClick() {
        jumpBridgesRepository.setConnections(emptyList())
        _state.update { it.copy(jumpBridgeNetworkState = JumpBridgeNetworkState.Empty) }
    }

    fun onJumpBridgeImportClick() {
        val network = (_state.value.jumpBridgeCopyState as? JumpBridgeCopyState.Copied)?.network ?: return
        importJumpBridges(network)
    }

    fun onJumpBridgeSearchImportClick() {
        val network = (_state.value.jumpBridgeSearchState as? JumpBridgeSearchState.SearchDone)?.network ?: return
        importJumpBridges(network)
    }

    private fun importJumpBridges(network: JumpBridgeNetwork) {
        jumpBridgesRepository.setConnections(network.connections)
        _state.update {
            it.copy(
                jumpBridgeNetworkState = JumpBridgeNetworkState.Loaded(network),
                jumpBridgeCopyState = JumpBridgeCopyState.NotCopied,
                jumpBridgeSearchState = JumpBridgeSearchState.NotSearched,
            )
        }
    }

    fun onJumpBridgeSearchClick() {
        _state.update { it.copy(isJumpBridgeSearchDialogShown = true) }
    }

    fun onIsJumpBridgeNetworkShownChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isJumpBridgeNetworkShown = enabled)
    }

    fun onJumpBridgeNetworkOpacityChange(percent: Int) {
        settings.intelMap = settings.intelMap.copy(jumpBridgeNetworkOpacity = percent)
    }

    fun onDialogDismissed() {
        _state.update { it.copy(isJumpBridgeSearchDialogShown = false) }
    }

    fun onJumpBridgeSearchDialogConfirmClick() {
        onDialogDismissed()
        if (_state.value.jumpBridgeSearchState !is JumpBridgeSearchState.NotSearched) return
        viewModelScope.launch {
            _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.Searching(0f, 0)) }
            jumpBridgesRepository.search().collect { searchState ->
                when (searchState) {
                    is JumpBridgesRepository.SearchState.Progress -> {
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.Searching(searchState.progress, searchState.connectionsCount)) }
                    }
                    JumpBridgesRepository.SearchState.Error -> {
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.SearchFailed) }
                        delay(2000)
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.NotSearched) }
                    }
                    is JumpBridgesRepository.SearchState.Result -> {
                        val network = JumpBridgeNetwork(searchState.connections)
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.SearchDone(network)) }
                    }
                }
            }
        }
    }
}
