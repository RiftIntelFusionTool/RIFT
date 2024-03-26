package dev.nohus.rift.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_info
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

data class DialogMessage(
    val title: String,
    val message: String,
    val type: MessageDialogType,
)

enum class MessageDialogType {
    Info, Warning
}

@Composable
fun WindowScope.RiftMessageDialog(
    dialog: DialogMessage,
    parentWindowState: RiftWindowState,
    onDismiss: () -> Unit,
) {
    RiftMessageDialog(
        title = dialog.title,
        message = dialog.message,
        type = dialog.type,
        parentWindowState = parentWindowState,
        onDismiss = onDismiss,
    )
}

@Composable
fun WindowScope.RiftMessageDialog(
    title: String,
    message: String,
    type: MessageDialogType,
    parentWindowState: RiftWindowState,
    onDismiss: () -> Unit,
) {
    val icon = when (type) {
        MessageDialogType.Info -> Res.drawable.window_info
        MessageDialogType.Warning -> Res.drawable.window_warning
    }
    RiftDialog(
        title = title,
        icon = icon,
        parentState = parentWindowState,
        state = rememberWindowState(width = 300.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = message,
                modifier = Modifier.heightIn(min = 60.dp),
            )
            RiftButton(
                text = "OK",
                cornerCut = ButtonCornerCut.Both,
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.large),
            )
        }
    }
}
