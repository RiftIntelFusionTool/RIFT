package dev.nohus.rift.settings.persistence

import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.utils.Size
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.serialization.Serializable

@Serializable
data class SettingsModel(
    val eveLogsDirectory: String? = null,
    val eveSettingsDirectory: String? = null,
    val isLoadOldMessagesEnabled: Boolean = false,
    val intelMap: IntelMap = IntelMap(),
    val authenticatedCharacters: Map<Int, SsoAuthentication> = emptyMap(),
    val intelChannels: List<IntelChannel> = emptyList(),
    val isRememberOpenWindows: Boolean = false,
    val isRememberWindowPlacement: Boolean = true,
    val openWindows: Set<RiftWindow> = emptySet(),
    val windowPlacements: Map<RiftWindow, WindowPlacement> = emptyMap(),
    val alwaysOnTopWindows: Set<RiftWindow> = emptySet(),
    val notificationEditPosition: Pos? = null,
    val notificationPosition: Pos? = null,
    val alerts: List<Alert> = emptyList(),
    val isSetupWizardFinished: Boolean = false,
    val isShowSetupWizardOnNextStart: Boolean = false,
    val isDisplayEveTime: Boolean = false,
    val jabberJidLocalPart: String? = null,
    val jabberPassword: String? = null,
    val jabberCollapsedGroups: List<String> = emptyList(),
    val isDemoMode: Boolean = false,
    val isSettingsReadFailure: Boolean = false,
    val isUsingDarkTrayIcon: Boolean = false,
    val intelReports: IntelReports = IntelReports(),
    val soundsVolume: Int = 100,
    val alertGroups: Set<String> = emptySet(),
    val configurationPack: ConfigurationPack? = null,
    val isConfigurationPackReminderDismissed: Boolean = false,
    val hiddenCharacterIds: List<Int> = emptyList(),
    val jumpBridgeNetwork: Map<String, String>? = null,
    val isUsingRiftAutopilotRoute: Boolean = true,
    val whatsNewVersion: String? = null,
)

@Serializable
enum class MapType {
    NewEden, Region
}

@Serializable
enum class MapStarColor {
    Actual, Security, IntelHostiles
}

@Serializable
data class IntelMap(
    val isUsingCompactMode: Boolean = false,
    val mapTypeStarColor: Map<MapType, MapStarColor> = emptyMap(),
    val intelExpireSeconds: Int = 5 * 60,
    val intelPopupTimeoutSeconds: Int = 60,
    val isCharacterFollowing: Boolean = true,
    val isInvertZoom: Boolean = false,
    val isJumpBridgeNetworkShown: Boolean = true,
    val jumpBridgeNetworkOpacity: Int = 100,
)

@Serializable
data class SsoAuthentication(
    val accessToken: String,
    val refreshToken: String,
    val expiration: Long,
)

@Serializable
data class IntelChannel(
    val name: String,
    val region: String,
)

@Serializable
data class WindowPlacement(
    val position: Pos,
    val size: Size,
)

@Serializable
data class IntelReports(
    val isUsingCompactMode: Boolean = false,
    val isShowingReporter: Boolean = true,
    val isShowingChannel: Boolean = true,
    val isShowingRegion: Boolean = false,
    val isShowingSystemDistance: Boolean = true,
)

@Serializable
enum class ConfigurationPack {
    Imperium,
}
