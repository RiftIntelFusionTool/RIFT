package dev.nohus.rift.settings.persistence

import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import java.nio.file.Path
import java.time.ZoneId
import kotlin.io.path.pathString

@Single
class Settings(
    private val persistence: SettingsPersistence,
) {
    private var model = persistence.load()
    private val _updateFlow = MutableStateFlow(model)
    val updateFlow = _updateFlow.asStateFlow()

    private fun update(update: SettingsModel.() -> SettingsModel) {
        val newModel = model.update()
        model = newModel
        _updateFlow.tryEmit(newModel)
        persistence.save(newModel)
    }

    var eveLogsDirectory: Path?
        get() = model.eveLogsDirectory?.let { Path.of(it) }
        set(value) = update { copy(eveLogsDirectory = value?.pathString) }

    var eveSettingsDirectory: Path?
        get() = model.eveSettingsDirectory?.let { Path.of(it) }
        set(value) = update { copy(eveSettingsDirectory = value?.pathString) }

    var isLoadOldMessagesEnabled: Boolean
        get() = model.isLoadOldMessagesEnabled
        set(value) = update { copy(isLoadOldMessagesEnabled = value) }

    var intelMap: IntelMap
        get() = model.intelMap
        set(value) = update { copy(intelMap = value) }

    var authenticatedCharacters: Map<Int, SsoAuthentication>
        get() = model.authenticatedCharacters
        set(value) = update { copy(authenticatedCharacters = value) }

    var intelChannels: List<IntelChannel>
        get() = model.intelChannels
        set(value) = update { copy(intelChannels = value) }

    var isRememberOpenWindows: Boolean
        get() = model.isRememberOpenWindows
        set(value) = update { copy(isRememberOpenWindows = value) }

    var isRememberWindowPlacement: Boolean
        get() = model.isRememberWindowPlacement
        set(value) = update { copy(isRememberWindowPlacement = value) }

    var openWindows: Set<RiftWindow>
        get() = model.openWindows
        set(value) = update { copy(openWindows = value) }

    var windowPlacements: Map<RiftWindow, WindowPlacement>
        get() = model.windowPlacements
        set(value) = update { copy(windowPlacements = value) }

    var alwaysOnTopWindows: Set<RiftWindow>
        get() = model.alwaysOnTopWindows
        set(value) = update { copy(alwaysOnTopWindows = value) }

    var lockedWindows: Set<RiftWindow>
        get() = model.lockedWindows
        set(value) = update { copy(lockedWindows = value) }

    var notificationEditPosition: Pos?
        get() = model.notificationEditPosition
        set(value) = update { copy(notificationEditPosition = value) }

    var notificationPosition: Pos?
        get() = model.notificationPosition
        set(value) = update { copy(notificationPosition = value) }

    var alerts: List<Alert>
        get() = model.alerts
        set(value) = update { copy(alerts = value) }

    var isSetupWizardFinished: Boolean
        get() = model.isSetupWizardFinished
        set(value) = update { copy(isSetupWizardFinished = value) }

    var isShowSetupWizardOnNextStart: Boolean
        get() = model.isShowSetupWizardOnNextStart
        set(value) = update { copy(isShowSetupWizardOnNextStart = value) }

    var isDemoMode: Boolean
        get() = model.isDemoMode
        set(value) = update { copy(isDemoMode = value) }

    var isDisplayEveTime: Boolean
        get() = model.isDisplayEveTime
        set(value) = update { copy(isDisplayEveTime = value) }

    val displayTimeZone: ZoneId
        get() = if (model.isDisplayEveTime) ZoneId.of("UTC") else ZoneId.systemDefault()

    var jabberJidLocalPart: String?
        get() = model.jabberJidLocalPart
        set(value) = update { copy(jabberJidLocalPart = value) }

    var jabberPassword: String?
        get() = model.jabberPassword
        set(value) = update { copy(jabberPassword = value) }

    var jabberCollapsedGroups: List<String>
        get() = model.jabberCollapsedGroups
        set(value) = update { copy(jabberCollapsedGroups = value) }

    var isSettingsReadFailure: Boolean
        get() = model.isSettingsReadFailure
        set(value) = update { copy(isSettingsReadFailure = value) }

    var isUsingDarkTrayIcon: Boolean
        get() = model.isUsingDarkTrayIcon
        set(value) = update { copy(isUsingDarkTrayIcon = value) }

    var intelReports: IntelReports
        get() = model.intelReports
        set(value) = update { copy(intelReports = value) }

    var intelFeed: IntelFeed
        get() = model.intelFeed
        set(value) = update { copy(intelFeed = value) }

    var soundsVolume: Int
        get() = model.soundsVolume
        set(value) = update { copy(soundsVolume = value) }

    var alertGroups: Set<String>
        get() = model.alertGroups
        set(value) = update { copy(alertGroups = value) }

    var configurationPack: ConfigurationPack?
        get() = model.configurationPack
        set(value) = update { copy(configurationPack = value) }

    var isConfigurationPackReminderDismissed: Boolean
        get() = model.isConfigurationPackReminderDismissed
        set(value) = update { copy(isConfigurationPackReminderDismissed = value) }

    var hiddenCharacterIds: List<Int>
        get() = model.hiddenCharacterIds
        set(value) = update { copy(hiddenCharacterIds = value) }

    var jumpBridgeNetwork: Map<String, String>?
        get() = model.jumpBridgeNetwork
        set(value) = update { copy(jumpBridgeNetwork = value) }

    var isUsingRiftAutopilotRoute: Boolean
        get() = model.isUsingRiftAutopilotRoute
        set(value) = update { copy(isUsingRiftAutopilotRoute = value) }

    var isSettingAutopilotToAll: Boolean
        get() = model.isSettingAutopilotToAll
        set(value) = update { copy(isSettingAutopilotToAll = value) }

    var whatsNewVersion: String?
        get() = model.whatsNewVersion
        set(value) = update { copy(whatsNewVersion = value) }

    var jumpRange: JumpRange?
        get() = model.jumpRange
        set(value) = update { copy(jumpRange = value) }

    var selectedPlanetTypes: List<Int>
        get() = model.selectedPlanetTypes
        set(value) = update { copy(selectedPlanetTypes = value) }

    var installationId: String?
        get() = model.installationId
        set(value) = update { copy(installationId = value) }

    var isShowingSystemDistance: Boolean
        get() = model.isShowingSystemDistance
        set(value) = update { copy(isShowingSystemDistance = value) }

    var isUsingJumpBridgesForDistance: Boolean
        get() = model.isUsingJumpBridgesForDistance
        set(value) = update { copy(isUsingJumpBridgesForDistance = value) }

    var intelExpireSeconds: Int
        get() = model.intelExpireSeconds
        set(value) = update { copy(intelExpireSeconds = value) }
}
