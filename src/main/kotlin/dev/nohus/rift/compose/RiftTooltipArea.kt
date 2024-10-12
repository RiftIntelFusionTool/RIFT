package dev.nohus.rift.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.zIndex
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.tooltip_pointer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Where on the content element the tooltip is anchored to
 */
enum class Anchor {
    Left, Middle, Right
}

@Composable
fun RiftTooltipArea(
    text: String?,
    modifier: Modifier = Modifier,
    contentAnchor: Anchor = Anchor.Middle,
    horizontalOffset: Dp = 0.dp,
    verticalOffset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    RiftTooltipArea(
        text = text?.let { AnnotatedString(it) },
        modifier = modifier,
        contentAnchor = contentAnchor,
        horizontalOffset = horizontalOffset,
        verticalOffset = verticalOffset,
        content = content,
    )
}

@Composable
fun RiftTooltipArea(
    text: AnnotatedString?,
    modifier: Modifier = Modifier,
    contentAnchor: Anchor = Anchor.Middle,
    horizontalOffset: Dp = 0.dp,
    verticalOffset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    RiftTooltipArea(
        tooltip = text?.let { @Composable { Text(it, Modifier.padding(Spacing.large)) } },
        modifier = modifier,
        contentAnchor = contentAnchor,
        horizontalOffset = horizontalOffset,
        verticalOffset = verticalOffset,
        content = content,
    )
}

@Composable
fun RiftTooltipArea(
    tooltip: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentAnchor: Anchor = Anchor.Middle,
    horizontalOffset: Dp = 0.dp,
    verticalOffset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    PointerTooltipArea(
        tooltip = tooltip?.let { { TooltipContent(it) } },
        topPointer = { AnchorPointer(isTop = true, x = it) },
        bottomPointer = { AnchorPointer(isTop = false, x = it) },
        pointerSize = IntSize(13, 9),
        contentAnchor = contentAnchor,
        offset = DpOffset(horizontalOffset, verticalOffset),
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun AnchorPointer(
    isTop: Boolean,
    x: Int?,
) {
    val alpha = if (x != null) 0.95f else 0f
    Image(
        painter = painterResource(Res.drawable.tooltip_pointer),
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer(
                translationX = x?.toFloat() ?: 0f,
                translationY = if (!isTop) -1f else 1f,
                alpha = alpha,
            )
            .modifyIf(isTop) { rotate(180f) }
            .zIndex(1f),
    )
}

@Composable
private fun TooltipContent(
    tooltip: @Composable () -> Unit,
) {
    val alpha = 0.95f
    Box(
        modifier = Modifier
            .background(RiftTheme.colors.windowBackground.copy(alpha = alpha))
            .border(1.dp, RiftTheme.colors.borderGrey.copy(alpha = alpha))
            .zIndex(0f),
    ) {
        tooltip()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PointerTooltipArea(
    tooltip: @Composable (() -> Unit)?,
    topPointer: @Composable (x: Int?) -> Unit,
    bottomPointer: @Composable (x: Int?) -> Unit,
    pointerSize: IntSize,
    contentAnchor: Anchor,
    offset: DpOffset = DpOffset.Zero,
    modifier: Modifier = Modifier,
    delayMillis: Int = 500,
    content: @Composable () -> Unit,
) {
    var parentBounds by remember { mutableStateOf(Rect.Zero) }
    var cursorPosition by remember { mutableStateOf(Offset.Zero) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job: Job? by remember { mutableStateOf(null) }

    fun startShowing() {
        if (job?.isActive == true) { // Don't restart the job if it's already active
            return
        }
        job = scope.launch {
            delay(delayMillis.toLong())
            isVisible = true
        }
    }

    fun hide() {
        job?.cancel()
        job = null
        isVisible = false
    }

    fun hideIfNotHovered(globalPosition: Offset) {
        if (!parentBounds.contains(globalPosition)) {
            hide()
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { parentBounds = it.boundsInWindow() }
            .onPointerEvent(PointerEventType.Enter) {
                cursorPosition = it.position
                if (!isVisible && !it.buttons.areAnyPressed) {
                    startShowing()
                }
            }
            .onPointerEvent(PointerEventType.Move) {
                cursorPosition = it.position
                if (!isVisible && !it.buttons.areAnyPressed) {
                    startShowing()
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                hideIfNotHovered(parentBounds.topLeft + it.position)
            }
            .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) {
                hide()
            },
    ) {
        content()
        if (tooltip != null && isVisible) {
            var pointerPosition: Pair<Int?, Int?> by remember { mutableStateOf(null to null) }
            Popup(
                popupPositionProvider = rememberPositionProvider(
                    pointerSize = pointerSize,
                    contentAnchor = contentAnchor,
                    offset = offset,
                    onPointerPositioned = { topX, bottomX -> pointerPosition = topX to bottomX },
                ),
                onDismissRequest = { isVisible = false },
            ) {
                var popupPosition by remember { mutableStateOf(Offset.Zero) }
                Box(
                    Modifier
                        .onGloballyPositioned { popupPosition = it.positionInWindow() }
                        .onPointerEvent(PointerEventType.Move) {
                            hideIfNotHovered(popupPosition + it.position)
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            hideIfNotHovered(popupPosition + it.position)
                        },
                ) {
                    val isPointerVisible = pointerPosition.first != null || pointerPosition.second != null
                    Column(
                        modifier = Modifier.alpha(if (isPointerVisible) 1f else 0f),
                    ) {
                        topPointer(pointerPosition.first)
                        tooltip()
                        bottomPointer(pointerPosition.second)
                    }
                }
            }
        }
    }
}

private val PointerEvent.position get() = changes.first().position

@Composable
private fun rememberPositionProvider(
    pointerSize: IntSize,
    contentAnchor: Anchor,
    offset: DpOffset = DpOffset.Zero,
    onPointerPositioned: (topX: Int?, bottomX: Int?) -> Unit,
): PopupPositionProvider {
    val offsetPx = with(LocalDensity.current) {
        IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    }
    return remember(pointerSize, contentAnchor, offsetPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val spaceUp = anchorBounds.top
                val halfWidth = popupContentSize.width / 2
                val halfPointerWidth = pointerSize.width / 2
                val canDisplayOnTop = spaceUp >= (popupContentSize.height + pointerSize.height)

                val x = when (contentAnchor) {
                    Anchor.Middle -> anchorBounds.center.x - halfWidth + offsetPx.x
                    Anchor.Left -> anchorBounds.left - halfWidth + offsetPx.x
                    Anchor.Right -> anchorBounds.right - halfWidth + offsetPx.x
                }
                val rightOverflow = x + popupContentSize.width - windowSize.width
                val leftOverflow = -x
                val pointerX = if (rightOverflow > 0) {
                    halfWidth - halfPointerWidth + rightOverflow
                } else if (leftOverflow > 0) {
                    halfWidth - halfPointerWidth - leftOverflow
                } else {
                    halfWidth - halfPointerWidth
                }
                val y = if (canDisplayOnTop) {
                    anchorBounds.top - popupContentSize.height + offsetPx.y
                } else {
                    anchorBounds.bottom + offsetPx.y
                }

                onPointerPositioned(
                    pointerX.takeIf { !canDisplayOnTop },
                    pointerX.takeIf { canDisplayOnTop },
                )
                return IntOffset(x, y)
            }
        }
    }
}
