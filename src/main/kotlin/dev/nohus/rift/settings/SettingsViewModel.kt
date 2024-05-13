package dev.nohus.rift.settings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.DetectEveSettingsDirectoryUseCase
import dev.nohus.rift.characters.GetEveCharactersSettingsUseCase
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.logs.DetectLogsDirectoryUseCase
import dev.nohus.rift.logs.GetChatLogsDirectoryUseCase
import dev.nohus.rift.repositories.ConfigurationPackRepository
import dev.nohus.rift.repositories.ConfigurationPackRepository.SuggestedIntelChannels
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.Pos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.koin.core.annotation.Single
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.pathString

@Single
class SettingsViewModel(
    private val settings: Settings,
    private val detectLogsDirectoryUseCase: DetectLogsDirectoryUseCase,
    private val detectEveSettingsDirectoryUseCase: DetectEveSettingsDirectoryUseCase,
    private val getChatLogsDirectoryUseCase: GetChatLogsDirectoryUseCase,
    private val getEveCharactersSettingsUseCase: GetEveCharactersSettingsUseCase,
    private val configurationPackRepository: ConfigurationPackRepository,
    solarSystemsRepository: SolarSystemsRepository,
) : ViewModel() {

    data class UiState(
        val intelChannels: List<IntelChannel>,
        val suggestedIntelChannels: SuggestedIntelChannels?,
        val regions: List<String>,
        val logsDirectory: String,
        val isLogsDirectoryValid: Boolean,
        val settingsDirectory: String,
        val isSettingsDirectoryValid: Boolean,
        val isLoadOldMessagesEnabled: Boolean,
        val isDisplayEveTime: Boolean,
        val isShowSetupWizardOnNextStartEnabled: Boolean,
        val isRememberOpenWindows: Boolean,
        val isRememberWindowPlacement: Boolean,
        val isEditNotificationWindowOpen: Boolean = false,
        val notificationEditPlacement: Pos? = null,
        val isUsingDarkTrayIcon: Boolean,
        val soundsVolume: Int,
        val configurationPack: ConfigurationPack?,
        val dialogMessage: DialogMessage? = null,
    )

    private val _state = MutableStateFlow(
        UiState(
            intelChannels = settings.intelChannels,
            suggestedIntelChannels = configurationPackRepository.getSuggestedIntelChannels(),
            regions = solarSystemsRepository.getKnownSpaceRegions().sorted(),
            logsDirectory = settings.eveLogsDirectory?.pathString ?: "",
            isLogsDirectoryValid = getChatLogsDirectoryUseCase(settings.eveLogsDirectory) != null,
            settingsDirectory = settings.eveSettingsDirectory?.pathString ?: "",
            isSettingsDirectoryValid = getEveCharactersSettingsUseCase(settings.eveSettingsDirectory).isNotEmpty(),
            isLoadOldMessagesEnabled = settings.isLoadOldMessagesEnabled,
            isDisplayEveTime = settings.isDisplayEveTime,
            isShowSetupWizardOnNextStartEnabled = settings.isShowSetupWizardOnNextStart,
            isRememberOpenWindows = settings.isRememberOpenWindows,
            isRememberWindowPlacement = settings.isRememberWindowPlacement,
            notificationEditPlacement = settings.notificationEditPosition,
            isUsingDarkTrayIcon = settings.isUsingDarkTrayIcon,
            soundsVolume = settings.soundsVolume,
            configurationPack = settings.configurationPack,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        intelChannels = settings.intelChannels,
                        suggestedIntelChannels = configurationPackRepository.getSuggestedIntelChannels(),
                        isLoadOldMessagesEnabled = settings.isLoadOldMessagesEnabled,
                        isDisplayEveTime = settings.isDisplayEveTime,
                        isShowSetupWizardOnNextStartEnabled = settings.isShowSetupWizardOnNextStart,
                        isRememberOpenWindows = settings.isRememberOpenWindows,
                        isRememberWindowPlacement = settings.isRememberWindowPlacement,
                        notificationEditPlacement = settings.notificationEditPosition,
                        isUsingDarkTrayIcon = settings.isUsingDarkTrayIcon,
                        soundsVolume = settings.soundsVolume,
                        configurationPack = settings.configurationPack,
                    )
                }
                val logsDirectory = settings.eveLogsDirectory
                if (logsDirectory?.pathString != _state.value.logsDirectory) {
                    _state.update {
                        it.copy(
                            logsDirectory = logsDirectory?.pathString ?: "",
                            isLogsDirectoryValid = getChatLogsDirectoryUseCase(logsDirectory) != null,
                        )
                    }
                }
                val settingsDirectory = settings.eveSettingsDirectory
                if (settingsDirectory?.pathString != _state.value.settingsDirectory) {
                    _state.update {
                        it.copy(
                            settingsDirectory = settingsDirectory?.pathString ?: "",
                            isSettingsDirectoryValid = getEveCharactersSettingsUseCase(settingsDirectory).isNotEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun onSuggestedIntelChannelsClick() {
        val channels = configurationPackRepository.getSuggestedIntelChannels()?.channels ?: emptyList()
        settings.intelChannels = (settings.intelChannels + channels).sortedBy { it.name }
    }

    fun onIntelChannelAdded(name: String, region: String) {
        val channel = IntelChannel(name, region)
        val channels = (settings.intelChannels + channel).sortedBy { it.name }
        settings.intelChannels = channels
    }

    fun onIntelChannelDelete(channel: IntelChannel) {
        settings.intelChannels -= channel
    }

    fun onLogsDirectoryChanged(text: String) {
        val directory = Path.of(text)
        settings.eveLogsDirectory = directory
        _state.update {
            it.copy(
                logsDirectory = text,
                isLogsDirectoryValid = getChatLogsDirectoryUseCase(directory) != null,
            )
        }
    }

    fun onDetectLogsDirectoryClick() {
        val logsDirectory = detectLogsDirectoryUseCase()
        if (logsDirectory == null) {
            val title = "Cannot find logs"
            val message =
                "Cannot find your EVE Online logs directory.\nPlease enter a path ending in \"EVE/logs\" manually."
            _state.update { it.copy(dialogMessage = DialogMessage(title, message, MessageDialogType.Warning)) }
        }
    }

    fun onSettingsDirectoryChanged(text: String) {
        val directory = Path.of(text)
        settings.eveSettingsDirectory = directory
        _state.update {
            it.copy(
                settingsDirectory = text,
                isSettingsDirectoryValid = getEveCharactersSettingsUseCase(directory).isNotEmpty(),
            )
        }
    }

    fun onDetectSettingsDirectoryClick() {
        val directory = detectEveSettingsDirectoryUseCase()
        if (directory == null) {
            val title = "Cannot find installation"
            val message =
                "Cannot find your EVE Online settings directory.\nPlease enter a path ending in \"CCP/EVE/[..]_tq_tranquility\" manually."
            _state.update { it.copy(dialogMessage = DialogMessage(title, message, MessageDialogType.Warning)) }
        }
    }

    fun onLoadOldMessagedChanged(enabled: Boolean) {
        settings.isLoadOldMessagesEnabled = enabled
    }

    fun onShowSetupWizardOnNextStartChanged(enabled: Boolean) {
        settings.isShowSetupWizardOnNextStart = enabled
    }

    fun onIsDisplayEveTimeChanged(enabled: Boolean) {
        settings.isDisplayEveTime = enabled
    }

    fun onRememberOpenWindowsChanged(enabled: Boolean) {
        settings.isRememberOpenWindows = enabled
    }

    fun onRememberWindowPlacementChanged(enabled: Boolean) {
        settings.isRememberWindowPlacement = enabled
    }

    fun onEditNotificationClick() {
        _state.update { it.copy(isEditNotificationWindowOpen = true) }
    }

    fun onEditNotificationDone(editPos: Pos?, pos: Pos?) {
        _state.update { it.copy(isEditNotificationWindowOpen = false) }
        if (editPos != null) {
            settings.notificationEditPosition = editPos
        }
        if (pos != null) {
            settings.notificationPosition = pos
        }
    }

    fun onIsUsingDarkTrayIconChanged(enabled: Boolean) {
        if (settings.isUsingDarkTrayIcon != enabled) {
            showRestartRequiredDialog("New tray icon will take effect after you restart the application.")
            settings.isUsingDarkTrayIcon = enabled
        }
    }

    fun onSoundsVolumeChange(volume: Int) {
        settings.soundsVolume = volume
    }

    fun onConfigurationPackChange(configurationPack: ConfigurationPack?) {
        if (settings.configurationPack != configurationPack) {
            showRestartRequiredDialog("Some elements of a configuration pack will only take effect after you restart the application.")
            settings.configurationPack = configurationPack
        }
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
    }

    private fun showRestartRequiredDialog(message: String) {
        viewModelScope.launch {
            delay(Duration.ofMillis(300))
            val dialogMessage = DialogMessage("Restart required", message, MessageDialogType.Info)
            _state.update { it.copy(dialogMessage = dialogMessage) }
        }
    }
}
