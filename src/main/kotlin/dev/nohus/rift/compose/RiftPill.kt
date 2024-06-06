package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RiftPill(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onHoverChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val borderColor = RiftTheme.colors.primary
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .background(RiftTheme.colors.backgroundPrimary, shape = shape)
            .hoverBackground(
                hoverColor = RiftTheme.colors.backgroundPrimaryLight,
                pressColor = RiftTheme.colors.backgroundPrimaryLight,
                shape = shape,
            )
            .onClick { onClick() }
            .onPointerEvent(PointerEventType.Enter) { onHoverChange(true) }
            .onPointerEvent(PointerEventType.Exit) { onHoverChange(false) }
            .modifyIf(isSelected) {
                border(1.dp, borderColor, shape)
            }
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
    ) {
        Text(
            text = text,
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}
