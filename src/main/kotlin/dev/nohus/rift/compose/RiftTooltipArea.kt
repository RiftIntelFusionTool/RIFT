package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.tooltip_pointer
import org.jetbrains.compose.resources.painterResource

enum class TooltipAnchor {
    BottomStart, BottomCenter, BottomEnd,
    TopStart, TopCenter, TopEnd
}

/**
 * @param anchor Where on the tooltip popup is the anchor triangle
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiftTooltipArea(
    tooltip: String,
    anchor: TooltipAnchor,
    modifier: Modifier = Modifier,
    contentAnchor: Alignment = Alignment.TopCenter,
    horizontalOffset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val pointerAlignment = when (anchor) {
        TooltipAnchor.BottomStart, TooltipAnchor.TopStart -> Alignment.Start
        TooltipAnchor.BottomCenter, TooltipAnchor.TopCenter -> Alignment.CenterHorizontally
        TooltipAnchor.BottomEnd, TooltipAnchor.TopEnd -> Alignment.End
    }
    val tooltipAlignment = when (anchor) {
        TooltipAnchor.BottomStart -> Alignment.TopEnd
        TooltipAnchor.BottomCenter -> Alignment.TopCenter
        TooltipAnchor.BottomEnd -> Alignment.TopStart
        TooltipAnchor.TopStart -> Alignment.BottomEnd
        TooltipAnchor.TopCenter -> Alignment.BottomCenter
        TooltipAnchor.TopEnd -> Alignment.BottomStart
    }
    val tooltipOffset = when (anchor) {
        TooltipAnchor.BottomStart, TooltipAnchor.TopStart -> (-6).dp
        TooltipAnchor.BottomCenter, TooltipAnchor.TopCenter -> 0.dp
        TooltipAnchor.BottomEnd, TooltipAnchor.TopEnd -> 6.dp
    }
    TooltipArea(
        tooltip = {
            val alpha = 0.95f
            Column {
                if (anchor in listOf(TooltipAnchor.TopStart, TooltipAnchor.TopCenter, TooltipAnchor.TopEnd)) {
                    Image(
                        painter = painterResource(Res.drawable.tooltip_pointer),
                        contentDescription = null,
                        modifier = Modifier
                            .align(pointerAlignment)
                            .graphicsLayer(translationY = 1f, alpha = alpha)
                            .rotate(180f)
                            .zIndex(1f),
                    )
                }
                Box(
                    modifier = Modifier
                        .background(RiftTheme.colors.windowBackground.copy(alpha = alpha))
                        .border(1.dp, RiftTheme.colors.borderGrey.copy(alpha = alpha))
                        .zIndex(0f),
                ) {
                    Text(tooltip, modifier = Modifier.padding(Spacing.large))
                }
                if (anchor in listOf(TooltipAnchor.BottomStart, TooltipAnchor.BottomCenter, TooltipAnchor.BottomEnd)) {
                    Image(
                        painter = painterResource(Res.drawable.tooltip_pointer),
                        contentDescription = null,
                        modifier = Modifier
                            .align(pointerAlignment)
                            .graphicsLayer(translationY = -1f, alpha = alpha),
                    )
                }
            }
        },
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = contentAnchor,
            alignment = tooltipAlignment,
            offset = DpOffset(tooltipOffset + horizontalOffset, 0.dp),
        ),
        modifier = modifier,
        content = content,
    )
}
