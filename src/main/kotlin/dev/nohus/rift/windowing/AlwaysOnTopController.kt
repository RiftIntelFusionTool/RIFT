package dev.nohus.rift.windowing

import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class AlwaysOnTopController(
    private val settings: Settings,
) {

    fun isAlwaysOnTop(window: RiftWindow?) = flow {
        if (window == null) {
            emit(false)
        } else {
            emit(window in settings.alwaysOnTopWindows)
            emitAll(settings.updateFlow.map { window in it.alwaysOnTopWindows })
        }
    }

    fun toggleAlwaysOnTop(window: RiftWindow?) {
        window ?: return
        if (window in settings.alwaysOnTopWindows) {
            settings.alwaysOnTopWindows -= window
        } else {
            settings.alwaysOnTopWindows += window
        }
    }
}
