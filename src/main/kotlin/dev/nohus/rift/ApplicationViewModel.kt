package dev.nohus.rift

import dev.nohus.rift.configurationpack.ShouldShowConfigurationPackReminderUseCase
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.singleinstance.SingleInstanceController
import dev.nohus.rift.startupwarning.GetStartupWarningsUseCase
import dev.nohus.rift.startupwarning.StartupWarningInputModel
import dev.nohus.rift.utils.CleanupTempFilesUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.DetectDirectoriesUseCase
import dev.nohus.rift.whatsnew.WhatsNewController
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class ApplicationViewModel(
    private val detectDirectoriesUseCase: DetectDirectoriesUseCase,
    private val backgroundProcesses: BackgroundProcesses,
    private val windowManager: WindowManager,
    private val singleInstanceController: SingleInstanceController,
    private val shouldShowConfigurationPackReminderUseCase: ShouldShowConfigurationPackReminderUseCase,
    private val getStartupWarnings: GetStartupWarningsUseCase,
    private val cleanupTempFilesUseCase: CleanupTempFilesUseCase,
    private val whatsNewController: WhatsNewController,
    private val operatingSystem: OperatingSystem,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val isAnotherInstanceDialogShown: Boolean,
        val isApplicationRunning: Boolean,
        val isTrayIconShown: Boolean,
        val isSetupWizardShown: Boolean,
        val isSplashScreenShown: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            isAnotherInstanceDialogShown = false,
            isApplicationRunning = true,
            isTrayIconShown = false,
            isSetupWizardShown = false,
            isSplashScreenShown = isSplashScreenEnabled(),
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
        logger.info { "Initializing RIFT ${BuildConfig.version} on $operatingSystem" }
        viewModelScope.launch {
            cleanupTempFilesUseCase()
        }
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
            if (isSplashScreenEnabled()) delay(3_500)
            windowManager.openInitialWindows()
            _state.update {
                it.copy(
                    isSplashScreenShown = false,
                    isTrayIconShown = settings.isSetupWizardFinished,
                    isSetupWizardShown = !settings.isSetupWizardFinished || settings.isShowSetupWizardOnNextStart,
                )
            }
            launch {
                settings.updateFlow.map { it.isSetupWizardFinished }.collect { finished ->
                    _state.update { it.copy(isTrayIconShown = finished) }
                }
            }
            launch(MainUIDispatcher) {
                val showReminder = shouldShowConfigurationPackReminderUseCase()
                if (showReminder) windowManager.onWindowOpen(RiftWindow.ConfigurationPackReminder)
            }
            launch(MainUIDispatcher) {
                val startupWarnings = getStartupWarnings()
                if (startupWarnings.isNotEmpty()) {
                    windowManager.onWindowOpen(RiftWindow.StartupWarning, inputModel = StartupWarningInputModel(startupWarnings))
                }
            }
            if (settings.isSetupWizardFinished) {
                whatsNewController.showIfRequired()
            } else {
                whatsNewController.resetWhatsNewVersion()
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
        logger.info { "Starting additional instance anyway" }
        _state.update { it.copy(isAnotherInstanceDialogShown = false) }
    }

    fun onQuit() {
        viewModelScope.launch {
            windowManager.saveWindowPlacements()
            _state.update { it.copy(isApplicationRunning = false) }
        }
    }

    private fun isSplashScreenEnabled(): Boolean {
        return !settings.skipSplashScreen
    }
}
