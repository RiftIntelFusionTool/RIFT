package dev.nohus.rift.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import com.formdev.flatlaf.FlatDarkLaf
import dev.nohus.rift.ApplicationViewModel
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.tray_tray_128
import dev.nohus.rift.generated.resources.tray_tray_16
import dev.nohus.rift.generated.resources.tray_tray_24
import dev.nohus.rift.generated.resources.tray_tray_32
import dev.nohus.rift.generated.resources.tray_tray_64
import dev.nohus.rift.generated.resources.tray_tray_dark_128
import dev.nohus.rift.generated.resources.tray_tray_dark_16
import dev.nohus.rift.generated.resources.tray_tray_dark_24
import dev.nohus.rift.generated.resources.tray_tray_dark_32
import dev.nohus.rift.generated.resources.tray_tray_dark_64
import dev.nohus.rift.generated.resources.window_characters
import dev.nohus.rift.generated.resources.window_chatchannels
import dev.nohus.rift.generated.resources.window_evemailtag
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.generated.resources.window_map
import dev.nohus.rift.generated.resources.window_quitgame
import dev.nohus.rift.generated.resources.window_rift_64
import dev.nohus.rift.generated.resources.window_satellite
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.repositories.ConfigurationPackRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.windowing.WindowManager
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import dorkbox.systemTray.util.SizeAndScaling
import io.sentry.Sentry
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.JSeparator

@Composable
fun RiftTray(
    viewModel: ApplicationViewModel,
    windowManager: WindowManager,
    isVisible: Boolean,
) {
    val operatingSystem = remember { koin.get<OperatingSystem>() }
    val settings = remember { koin.get<Settings>() }
    val configurationPackRepository = remember { koin.get<ConfigurationPackRepository>() }
    if (isVisible) {
        initialize(
            operatingSystem = operatingSystem,
            isUsingDarkTrayIcon = settings.isUsingDarkTrayIcon,
            windowManager = windowManager,
            isJabberEnabled = configurationPackRepository.isJabberEnabled(),
            onQuitClick = viewModel::onQuit,
        )
    }
}

private var currentSystemTray: SystemTray? = null

@Composable
private fun initialize(
    operatingSystem: OperatingSystem,
    isUsingDarkTrayIcon: Boolean,
    windowManager: WindowManager,
    isJabberEnabled: Boolean,
    onQuitClick: () -> Unit,
) {
    if (currentSystemTray == null) {
        if (operatingSystem == OperatingSystem.Windows) {
            // On Windows the tray menu uses Swing
            FlatDarkLaf.setup() // Dark theme for Swing
            SizeAndScaling.TRAY_MENU_SIZE = 32 // Bigger menu icons
        }

        SystemTray.APP_NAME = "RIFT"
        val systemTray: SystemTray? = SystemTray.get()
        if (systemTray == null) {
            Sentry.captureMessage("System tray failed to initialize")
        } else {
            systemTray.setImage(getBestTrayIcon(systemTray, isUsingDarkTrayIcon))

            if (operatingSystem == OperatingSystem.Windows) {
                // On Windows the tooltip shows on hover
                systemTray.setTooltip("RIFT")
                // On Windows we want an icon
                systemTray.menu.add(MenuItem("RIFT", getImage(Res.drawable.window_rift_64)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Neocom) })
            } else {
                systemTray.menu.add(MenuItem("RIFT") { windowManager.onWindowOpen(WindowManager.RiftWindow.Neocom) })
            }
            systemTray.menu.add(JSeparator())
            systemTray.menu.add(MenuItem("Alerts", getImage(Res.drawable.window_loudspeaker_icon)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Alerts) })
            systemTray.menu.add(MenuItem("Intel Reports", getImage(Res.drawable.window_satellite)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Intel) })
            systemTray.menu.add(MenuItem("Intel Map", getImage(Res.drawable.window_map)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Map) })
            systemTray.menu.add(MenuItem("Characters", getImage(Res.drawable.window_characters)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Characters) })
            if (isJabberEnabled) {
                systemTray.menu.add(MenuItem("Pings", getImage(Res.drawable.window_sovereignty)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Pings) })
                systemTray.menu.add(MenuItem("Jabber", getImage(Res.drawable.window_chatchannels)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Jabber) })
            }
            systemTray.menu.add(MenuItem("Settings", getImage(Res.drawable.window_settings)) { windowManager.onWindowOpen(WindowManager.RiftWindow.Settings) })
            systemTray.menu.add(MenuItem("About", getImage(Res.drawable.window_evemailtag)) { windowManager.onWindowOpen(WindowManager.RiftWindow.About) })
            systemTray.menu.add(JSeparator())
            systemTray.menu.add(MenuItem("Quit", getImage(Res.drawable.window_quitgame)) { onQuitClick() })

            currentSystemTray = systemTray
        }
    }
}

@Composable
private fun getBestTrayIcon(systemTray: SystemTray, isUsingDarkTrayIcon: Boolean): Image {
    val icons = listOf(16, 24, 32, 64, 128)
    val requestedSize = systemTray.trayImageSize
    val bestSize = icons.firstOrNull { it >= requestedSize } ?: icons.last()
    val resource = if (isUsingDarkTrayIcon) {
        when (bestSize) {
            16 -> Res.drawable.tray_tray_dark_16
            24 -> Res.drawable.tray_tray_dark_24
            32 -> Res.drawable.tray_tray_dark_32
            64 -> Res.drawable.tray_tray_dark_64
            else -> Res.drawable.tray_tray_dark_128
        }
    } else {
        when (bestSize) {
            16 -> Res.drawable.tray_tray_16
            24 -> Res.drawable.tray_tray_24
            32 -> Res.drawable.tray_tray_32
            64 -> Res.drawable.tray_tray_64
            else -> Res.drawable.tray_tray_128
        }
    }
    return getImage(resource)
}

@Composable
private fun getImage(resource: DrawableResource): BufferedImage {
    return imageResource(resource).toAwtImage()
}
