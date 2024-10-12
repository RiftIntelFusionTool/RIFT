package dev.nohus.rift.compose

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_buttonglow
import dev.nohus.rift.windowing.LocalRiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiftImageButton(
    resource: DrawableResource,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconPadding: Dp = 0.dp,
    highlightModifier: Float = 1f,
) {
    val windowOpenTimestamp = LocalRiftWindowState.current?.openTimestamp
    key(windowOpenTimestamp) { // This is to clear hover / press states and animations when window is reopened
        val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
        val transition = updateTransition(pointerInteractionStateHolder.current)
        val highlightAlpha by transition.animateFloat {
            when (it) {
                PointerInteractionState.Normal -> 0f * highlightModifier
                PointerInteractionState.Hover -> 0.5f * highlightModifier
                PointerInteractionState.Press -> 1f * highlightModifier
            }
        }
        val iconAlpha by transition.animateFloat {
            when (it) {
                PointerInteractionState.Normal -> 0.75f
                PointerInteractionState.Hover -> 1f
                PointerInteractionState.Press -> 1f
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .pointerInteraction(pointerInteractionStateHolder)
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .onClick(onClick = onClick),
        ) {
            Image(
                painter = painterResource(Res.drawable.window_buttonglow),
                contentDescription = null,
                modifier = Modifier
                    .size(size + iconPadding * 2)
                    .alpha(highlightAlpha),
            )
            Image(
                painter = painterResource(resource),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .alpha(iconAlpha),
            )
        }
    }
}
