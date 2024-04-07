package dev.nohus.rift

import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.singleinstance.SingleInstanceController
import dev.nohus.rift.utils.directories.DetectDirectoriesUseCase
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class ApplicationViewModel(
    private val detectDirectoriesUseCase: DetectDirectoriesUseCase,
    private val backgroundProcesses: BackgroundProcesses,
    private val windowManager: WindowManager,
    private val singleInstanceController: SingleInstanceController,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val isAnotherInstanceDialogShown: Boolean,
        val isApplicationRunning: Boolean,
        val isTrayIconShown: Boolean,
        val isSetupWizardShown: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            isAnotherInstanceDialogShown = false,
            isApplicationRunning = true,
            isTrayIconShown = settings.isSetupWizardFinished,
            isSetupWizardShown = !settings.isSetupWizardFinished || settings.isShowSetupWizardOnNextStart,
        ),
    )
    val state = _state.asStateFlow()

    init {
        val isAnotherInstanceRunning = singleInstanceController.isInstanceRunning()
        _state.update { it.copy(isAnotherInstanceDialogShown = isAnotherInstanceRunning) }
        if (!isAnotherInstanceRunning) {
            initializeApplication()
        } else {
            viewModelScope.launch {
                state.map { it.isAnotherInstanceDialogShown }.filter { !it }.collect {
                    initializeApplication()
                }
            }
        }
    }

    private fun initializeApplication() {
        detectDirectoriesUseCase()
        viewModelScope.launch {
            backgroundProcesses.start()
        }
        viewModelScope.launch {
            while (true) {
                delay(10_000)
                windowManager.saveWindowPlacements()
            }
        }
        viewModelScope.launch {
            settings.updateFlow.map { it.isSetupWizardFinished }.collect { finished ->
                _state.update { it.copy(isTrayIconShown = finished) }
            }
        }
    }

    fun onWizardCloseRequest() {
        if (settings.isSetupWizardFinished) {
            _state.update { it.copy(isSetupWizardShown = false) }
        } else {
            _state.update { it.copy(isApplicationRunning = false) }
        }
    }

    fun onSingleInstanceRunAnywayClick() {
        _state.update { it.copy(isAnotherInstanceDialogShown = false) }
    }

    fun onQuit() {
        viewModelScope.launch {
            windowManager.saveWindowPlacements()
            _state.update { it.copy(isApplicationRunning = false) }
        }
    }
}
