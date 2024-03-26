package dev.nohus.rift.wizard

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun WizardWindowWrapper(
    isVisible: Boolean,
    onCloseRequest: () -> Unit,
) {
    if (isVisible) {
        WizardWindow(
            windowState = RiftWindowState(
                windowState = rememberWindowState(width = 550.dp, height = 300.dp),
                isVisible = true,
                minimumSize = 550 to 300,
            ),
            onCloseRequest = onCloseRequest,
        )
    }
}
