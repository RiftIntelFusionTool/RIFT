package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkText(
    text: String,
    style: TextStyle = RiftTheme.typography.bodyLink,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val coloredStyle = if (pointerInteractionStateHolder.isHovered) {
        style.copy(color = RiftTheme.colors.textLink, textDecoration = TextDecoration.Underline)
    } else {
        style.copy(color = RiftTheme.colors.textLink)
    }
    Text(
        text = text,
        style = coloredStyle,
        modifier = modifier
            .pointerInteraction(pointerInteractionStateHolder)
            .pointerHoverIcon(PointerIcon(Cursors.hand))
            .onClick { onClick() },
    )
}
