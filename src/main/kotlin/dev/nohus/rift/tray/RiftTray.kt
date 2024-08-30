package dev.nohus.rift.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import com.formdev.flatlaf.FlatDarkLaf
import dev.nohus.rift.ApplicationViewModel
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
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
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.generated.resources.window_bleedchannel
import dev.nohus.rift.generated.resources.window_characters
import dev.nohus.rift.generated.resources.window_chatchannels
import dev.nohus.rift.generated.resources.window_evemailtag
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.generated.resources.window_map
import dev.nohus.rift.generated.resources.window_quitgame
import dev.nohus.rift.generated.resources.window_rift_64
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.tray.TrayMenuItem.Separator
import dev.nohus.rift.tray.TrayMenuItem.TrayMenuTextItem
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.windowing.WindowManager
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import dorkbox.systemTray.util.SizeAndScaling
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.JSeparator

private val logger = KotlinLogging.logger {}

@Composable
fun ApplicationScope.RiftTray(
    viewModel: ApplicationViewModel,
    windowManager: WindowManager,
    isVisible: Boolean,
) {
    val operatingSystem = remember { koin.get<OperatingSystem>() }
    val settings = remember { koin.get<Settings>() }
    val configurationPackRepository = remember { koin.get<ConfigurationPackRepository>() }
    if (isVisible) {
        val items = getTrayMenuItems(
            operatingSystem = operatingSystem,
            windowManager = windowManager,
            isJabberEnabled = configurationPackRepository.isJabberEnabled(),
            onQuitClick = viewModel::onQuit,
        )
        val didInitialize = initialize(
            items = items,
            operatingSystem = operatingSystem,
            isUsingDarkTrayIcon = settings.isUsingDarkTrayIcon,
        )
        if (!didInitialize) {
            logger.info { "Falling back to AWT tray icon" }
            AwtTrayIcon(
                items = items,
                isUsingDarkTrayIcon = settings.isUsingDarkTrayIcon,
                windowManager = windowManager,
            )
        }
    }
}

private var currentSystemTray: SystemTray? = null

sealed interface TrayMenuItem {
    data class TrayMenuTextItem(
        val text: String,
        val drawable: DrawableResource?,
        val action: () -> Unit,
    ) : TrayMenuItem

    data object Separator : TrayMenuItem
}

private fun getTrayMenuItems(
    operatingSystem: OperatingSystem,
    windowManager: WindowManager,
    isJabberEnabled: Boolean,
    onQuitClick: () -> Unit,
): List<TrayMenuItem> {
    return buildList {
        if (operatingSystem == OperatingSystem.Windows) {
            // On Windows we want an icon
            add(TrayMenuTextItem("RIFT", Res.drawable.window_rift_64) { windowManager.onWindowOpen(WindowManager.RiftWindow.Neocom) })
        } else {
            add(TrayMenuTextItem("RIFT", null) { windowManager.onWindowOpen(WindowManager.RiftWindow.Neocom) })
        }
        add(Separator)
        add(TrayMenuTextItem("Alerts", Res.drawable.window_loudspeaker_icon) { windowManager.onWindowOpen(WindowManager.RiftWindow.Alerts) })
        add(TrayMenuTextItem("Map", Res.drawable.window_map) { windowManager.onWindowOpen(WindowManager.RiftWindow.Map) })
        add(TrayMenuTextItem("Intel Feed", Res.drawable.window_map) { windowManager.onWindowOpen(WindowManager.RiftWindow.IntelFeed) })
        add(TrayMenuTextItem("Intel Reports", Res.drawable.window_bleedchannel) { windowManager.onWindowOpen(WindowManager.RiftWindow.IntelReports) })
        add(TrayMenuTextItem("Characters", Res.drawable.window_characters) { windowManager.onWindowOpen(WindowManager.RiftWindow.Characters) })
        add(TrayMenuTextItem("Assets", Res.drawable.window_assets) { windowManager.onWindowOpen(WindowManager.RiftWindow.Assets) })
        if (isJabberEnabled) {
            add(TrayMenuTextItem("Pings", Res.drawable.window_sovereignty) { windowManager.onWindowOpen(WindowManager.RiftWindow.Pings) })
            add(TrayMenuTextItem("Jabber", Res.drawable.window_chatchannels) { windowManager.onWindowOpen(WindowManager.RiftWindow.Jabber) })
        }
        add(TrayMenuTextItem("Settings", Res.drawable.window_settings) { windowManager.onWindowOpen(WindowManager.RiftWindow.Settings) })
        add(TrayMenuTextItem("About", Res.drawable.window_evemailtag) { windowManager.onWindowOpen(WindowManager.RiftWindow.About) })
        add(Separator)
        add(TrayMenuTextItem("Quit", Res.drawable.window_quitgame) { onQuitClick() })
    }
}

@Composable
private fun initialize(
    items: List<TrayMenuItem>,
    operatingSystem: OperatingSystem,
    isUsingDarkTrayIcon: Boolean,
): Boolean {
    return if (currentSystemTray == null) {
        if (operatingSystem == OperatingSystem.Windows) {
            // On Windows the tray menu uses Swing
            FlatDarkLaf.setup() // Dark theme for Swing
            SizeAndScaling.TRAY_MENU_SIZE = 32 // Bigger menu icons
        }

        SystemTray.APP_NAME = "RIFT"
        val systemTray: SystemTray? = SystemTray.get()
        if (systemTray == null) {
            logger.error { "System tray failed to initialize" }
            false
        } else {
            val icon = getBestTrayIcon(systemTray, isUsingDarkTrayIcon)
            try {
                systemTray.setImage(icon)
                if (operatingSystem == OperatingSystem.Windows) {
                    // On Windows the tooltip shows on hover
                    systemTray.setTooltip("RIFT")
                }
            } catch (error: Error) {
                logger.error(error) { "System tray crashed" }
                return false
            }
            for (item in items) {
                when (item) {
                    is TrayMenuTextItem -> {
                        systemTray.menu.add(MenuItem(item.text, item.drawable?.let { getImage(it) }) { item.action() })
                    }
                    Separator -> {
                        systemTray.menu.add(JSeparator())
                    }
                }
            }
            currentSystemTray = systemTray
            true
        }
    } else {
        true
    }
}

@Composable
private fun ApplicationScope.AwtTrayIcon(
    items: List<TrayMenuItem>,
    isUsingDarkTrayIcon: Boolean,
    windowManager: WindowManager,
) {
    val icon = if (isUsingDarkTrayIcon) {
        Res.drawable.tray_tray_dark_128
    } else {
        Res.drawable.tray_tray_128
    }
    Tray(
        icon = painterResource(icon),
        state = rememberTrayState(),
        tooltip = "RIFT",
        onAction = { windowManager.onWindowOpen(WindowManager.RiftWindow.Neocom) },
        menu = {
            for (item in items) {
                when (item) {
                    is TrayMenuTextItem -> {
                        Item(item.text, onClick = item.action)
                    }
                    Separator -> {
                        Separator()
                    }
                }
            }
        },
    )
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
