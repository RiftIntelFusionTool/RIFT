package dev.nohus.rift.wizard

import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import dev.nohus.rift.Event
import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.files.GetEveCharactersSettingsUseCase
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.logs.GetChatLogsDirectoryUseCase
import dev.nohus.rift.settings.SettingsInputModel
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import kotlin.io.path.absolutePathString

@Factory
class WizardViewModel(
    private val getChatLogsDirectoryUseCase: GetChatLogsDirectoryUseCase,
    private val getEveCharactersSettingsUseCase: GetEveCharactersSettingsUseCase,
    private val settings: Settings,
    private val windowManager: WindowManager,
    private val localCharactersRepository: LocalCharactersRepository,
    private val appDirectories: AppDirectories,
    private val configurationPackRepository: ConfigurationPackRepository,
) : ViewModel() {

    data class UiState(
        val step: WizardStep = WizardStep.Welcome,
        val onFinishedEvent: Event? = null,
        val dialogMessage: DialogMessage? = null,
    )

    sealed interface WizardStep {
        data object Welcome : WizardStep

        data class EveInstallation(
            val state: EveInstallationState,
        ) : WizardStep

        data class Characters(
            val characterCount: Int,
            val authenticatedCharacterCount: Int,
        ) : WizardStep

        data class ConfigurationPacks(
            val pack: ConfigurationPack?,
        ) : WizardStep

        data class IntelChannels(
            val hasChannels: Boolean,
        ) : WizardStep

        data object Finish : WizardStep
    }

    enum class EveInstallationState {
        None, Detected, Set
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var typedText = ""

    init {
        if (settings.isDemoMode) {
            settings.isDemoMode = false
            settings.isSetupWizardFinished = false
        }
        checkSettingsReadFailure()
        viewModelScope.launch {
            settings.updateFlow.collect {
                val step = _state.value.step
                if (step is WizardStep.EveInstallation) {
                    val isValid = isEveInstallationValid()
                    if (isValid) {
                        if (step.state == EveInstallationState.None) {
                            _state.update { it.copy(step = WizardStep.EveInstallation(state = EveInstallationState.Set)) }
                        }
                    } else {
                        _state.update { it.copy(step = WizardStep.EveInstallation(state = EveInstallationState.None)) }
                    }
                } else if (step is WizardStep.IntelChannels) {
                    val hasChannels = settings.intelChannels.isNotEmpty()
                    _state.update { it.copy(step = WizardStep.IntelChannels(hasChannels)) }
                }
            }
        }
        viewModelScope.launch {
            localCharactersRepository.characters.collect {
                val step = _state.value.step
                if (step is WizardStep.Characters) {
                    _state.update { it.copy(step = getCharactersStep()) }
                }
            }
        }
    }

    private fun checkSettingsReadFailure() {
        if (settings.isSettingsReadFailure) {
            settings.isSettingsReadFailure = false
            _state.update {
                it.copy(
                    dialogMessage = DialogMessage(
                        title = "Can't read settings",
                        message = "Your settings couldn't be read and were reset. Your original settings file was backed up, and you can find it under:\n${appDirectories.getAppDataDirectory().absolutePathString()}",
                        type = MessageDialogType.Warning,
                    ),
                )
            }
        }
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
    }

    fun onSetEveInstallationClick() {
        windowManager.onWindowOpen(RiftWindow.Settings, SettingsInputModel.EveInstallation)
    }

    fun onCharactersClick() {
        windowManager.onWindowOpen(RiftWindow.Characters)
    }

    fun onConfigurationPackChange(configurationPack: ConfigurationPack?) {
        val step = _state.value.step as? WizardStep.ConfigurationPacks ?: return
        _state.update { it.copy(step = step.copy(pack = configurationPack)) }
    }

    fun onSetIntelChannelsClick() {
        windowManager.onWindowOpen(RiftWindow.Settings, SettingsInputModel.IntelChannels)
    }

    fun onContinueClick() {
        when (val step = _state.value.step) {
            WizardStep.Welcome -> {
                val isValid = isEveInstallationValid()
                val state = if (isValid) EveInstallationState.Detected else EveInstallationState.None
                _state.update { it.copy(step = WizardStep.EveInstallation(state = state)) }
            }

            is WizardStep.EveInstallation -> {
                windowManager.onWindowClose(RiftWindow.Settings)
                _state.update { it.copy(step = getCharactersStep()) }
            }

            is WizardStep.Characters -> {
                windowManager.onWindowClose(RiftWindow.Characters)
                val suggestedPack = configurationPackRepository.getSuggestedPack()
                if (suggestedPack != null) {
                    _state.update { it.copy(step = WizardStep.ConfigurationPacks(pack = suggestedPack)) }
                } else {
                    _state.update { it.copy(step = getIntelChannelsStep()) }
                }
            }

            is WizardStep.ConfigurationPacks -> {
                configurationPackRepository.set(step.pack)
                _state.update { it.copy(step = getIntelChannelsStep()) }
            }

            is WizardStep.IntelChannels -> {
                windowManager.onWindowClose(RiftWindow.Settings)
                settings.isSetupWizardFinished = true
                settings.isShowSetupWizardOnNextStart = false
                _state.update { it.copy(step = WizardStep.Finish) }
            }

            WizardStep.Finish -> {
                windowManager.onWindowOpen(RiftWindow.Neocom)
                _state.update { it.copy(onFinishedEvent = Event()) }
            }
        }
    }

    fun onKeyEvent(event: KeyEvent) {
        if (event.isTypedEvent) {
            val character = event.utf16CodePoint.toChar()
            if (character.isLetter()) {
                typedText += character
                if (typedText.takeLast(4) == "demo") {
                    startDemoMode()
                }
            }
        }
    }

    private fun startDemoMode() {
        settings.isSetupWizardFinished = true
        settings.isDemoMode = true
        windowManager.onWindowOpen(RiftWindow.Neocom)
        _state.update { it.copy(onFinishedEvent = Event()) }
    }

    private fun isEveInstallationValid(): Boolean {
        val isLogsDirectoryValid = getChatLogsDirectoryUseCase(settings.eveLogsDirectory) != null
        val isSettingsDirectoryValid = getEveCharactersSettingsUseCase(settings.eveSettingsDirectory).isNotEmpty()
        return isLogsDirectoryValid && isSettingsDirectoryValid
    }

    private fun getCharactersStep(): WizardStep.Characters {
        val characters = localCharactersRepository.characters.value
        val characterCount = characters.count()
        val authenticatedCharacterCount = characters.count { it.isAuthenticated }
        return WizardStep.Characters(characterCount, authenticatedCharacterCount)
    }

    private fun getIntelChannelsStep(): WizardStep.IntelChannels {
        return WizardStep.IntelChannels(hasChannels = settings.intelChannels.isNotEmpty())
    }
}
