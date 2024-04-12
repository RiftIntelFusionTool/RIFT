package dev.nohus.rift.settings.persistence

import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import java.io.File
import java.time.ZoneId

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

    var eveLogsDirectory: File?
        get() = model.eveLogsDirectory?.let { File(it) }
        set(value) = update { copy(eveLogsDirectory = value?.path) }

    var eveSettingsDirectory: File?
        get() = model.eveSettingsDirectory?.let { File(it) }
        set(value) = update { copy(eveSettingsDirectory = value?.path) }

    var isLoadOldMessagesEnabled: Boolean
        get() = model.isLoadOldMessagesEnabled
        set(value) = update { copy(isLoadOldMessagesEnabled = value) }

    var intelMap: IntelMap
        get() = model.intelMap.copy(mapTypeStarColor = model.intelMap.mapTypeStarColor.withDefault { MapStarColor.Security })
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

    var soundsVolume: Int
        get() = model.soundsVolume
        set(value) = update { copy(soundsVolume = value) }

    var alertGroups: Set<String>
        get() = model.alertGroups
        set(value) = update { copy(alertGroups = value) }

    var configurationPack: ConfigurationPack?
        get() = model.configurationPack
        set(value) = update { copy(configurationPack = value) }

    var hiddenCharacterIds: List<Int>
        get() = model.hiddenCharacterIds
        set(value) = update { copy(hiddenCharacterIds = value) }
}
