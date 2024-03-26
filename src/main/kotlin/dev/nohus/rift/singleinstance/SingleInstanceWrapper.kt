package dev.nohus.rift.singleinstance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun SingleInstanceWrapper(
    onRunAnywayClick: () -> Unit,
    onCloseRequest: () -> Unit,
) {
    SingleInstanceWindow(
        windowState = RiftWindowState(
            windowState = rememberWindowState(width = 300.dp, height = Dp.Unspecified),
            isVisible = true,
            minimumSize = 300 to 100,
        ),
        onRunAnywayClick = onRunAnywayClick,
        onCloseRequest = onCloseRequest,
    )
}
