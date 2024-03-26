package dev.nohus.rift.alerts.creategroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.windowing.WindowManager

@Composable
fun WindowScope.CreateGroupDialog(
    inputModel: CreateGroupInputModel,
    parentWindowState: WindowManager.RiftWindowState,
    onDismiss: () -> Unit,
    onConfirmClick: (name: String) -> Unit,
) {
    val title = when (inputModel) {
        CreateGroupInputModel.New -> "Create group"
        is CreateGroupInputModel.Rename -> "Rename group"
    }
    RiftDialog(
        title = title,
        icon = Res.drawable.window_loudspeaker_icon,
        parentState = parentWindowState,
        state = rememberWindowState(width = 300.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        CreateGroupDialogContent(
            inputModel = inputModel,
            onCancelClick = onDismiss,
            onConfirmClick = onConfirmClick,
        )
    }
}

@Composable
private fun CreateGroupDialogContent(
    inputModel: CreateGroupInputModel,
    onCancelClick: () -> Unit,
    onConfirmClick: (name: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (inputModel is CreateGroupInputModel.New) {
            Text(
                text = "Groups allow you to organize your alerts.",
            )
        }
        val initialText = (inputModel as? CreateGroupInputModel.Rename)?.name ?: ""
        var text by remember { mutableStateOf(initialText) }
        RiftTextField(
            text = text,
            placeholder = "Group name",
            onTextChanged = { text = it.take(24) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            RiftButton(
                text = "Cancel",
                cornerCut = ButtonCornerCut.BottomLeft,
                type = ButtonType.Secondary,
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
            val label = when (inputModel) {
                CreateGroupInputModel.New -> "Create"
                is CreateGroupInputModel.Rename -> "Rename"
            }
            RiftButton(
                text = label,
                onClick = { onConfirmClick(text) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
