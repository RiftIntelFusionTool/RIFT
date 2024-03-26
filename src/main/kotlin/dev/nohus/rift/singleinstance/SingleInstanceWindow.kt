package dev.nohus.rift.singleinstance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.windowing.WindowManager

@Composable
fun SingleInstanceWindow(
    windowState: WindowManager.RiftWindowState,
    onRunAnywayClick: () -> Unit,
    onCloseRequest: () -> Unit,
) {
    RiftWindow(
        title = "RIFT Intel Fusion Tool",
        icon = Res.drawable.window_warning,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        SingleInstanceContent(
            onRunAnywayClick = onRunAnywayClick,
            onCloseClick = onCloseRequest,
        )
    }
}

@Composable
private fun SingleInstanceContent(
    onRunAnywayClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = "RIFT is already running. Starting multiple instances is not recommended.",
            style = RiftTheme.typography.bodyPrimary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Spacer(Modifier.weight(1f))
            RiftButton(
                text = "Run anyway",
                type = ButtonType.Negative,
                cornerCut = ButtonCornerCut.None,
                onClick = onRunAnywayClick,
            )
            RiftButton(
                text = "Close",
                onClick = onCloseClick,
            )
        }
    }
}
