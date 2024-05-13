package dev.nohus.rift.whatsnew

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import org.koin.core.annotation.Single

@Single
class WhatsNewController(
    private val windowManager: WindowManager,
    private val settings: Settings,
) {

    fun showIfRequired() {
        val lastShownVersion = settings.whatsNewVersion
        val currentVersion = BuildConfig.version
        if (lastShownVersion != currentVersion) {
            windowManager.onWindowOpen(RiftWindow.WhatsNew)
        }
        resetWhatsNewVersion()
    }

    fun resetWhatsNewVersion() {
        settings.whatsNewVersion = BuildConfig.version
    }
}
