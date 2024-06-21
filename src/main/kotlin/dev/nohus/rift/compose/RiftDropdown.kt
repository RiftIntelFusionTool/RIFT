package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.dropdown_chevron
import dev.nohus.rift.generated.resources.window_buttonglow
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun <T> RiftDropdown(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    getItemName: (T) -> String,
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
    height: Dp = 32.dp,
) {
    var dismissedTimestamp by remember { mutableStateOf(Instant.EPOCH) }
    var isExpanded by remember { mutableStateOf(false) }

    val shape = when (isExpanded) {
        false -> CutCornerShape(bottomEnd = 9.dp)
        true -> RectangleShape
    }

    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val transition = updateTransition(pointerInteractionStateHolder.current)
    val colorTransitionSpec = getDropdownTransitionSpec<Color>()
    val backgroundColor by animateColorAsState(
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        targetValue = when (isExpanded) {
            false -> RiftTheme.colors.windowBackground
            true -> RiftTheme.colors.windowBackgroundActive
        },
    )
    val borderColor by animateColorAsState(
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        targetValue = when (isExpanded) {
            false -> RiftTheme.colors.borderGrey
            true -> RiftTheme.colors.borderGreyDropdown
        },
    )
    val textColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            PointerInteractionState.Normal -> RiftTheme.colors.textPrimary
            PointerInteractionState.Hover -> RiftTheme.colors.textHighlighted
            PointerInteractionState.Press -> RiftTheme.colors.textHighlighted
        }
    }
    val highlightAlpha by transition.animateFloat {
        when (it) {
            PointerInteractionState.Normal -> 0f
            PointerInteractionState.Hover -> 0.5f
            PointerInteractionState.Press -> 0.5f
        }
    }
    val chevronAlpha by transition.animateFloat {
        when (it) {
            PointerInteractionState.Normal -> 0.8f
            PointerInteractionState.Hover -> 1.0f
            PointerInteractionState.Press -> 1.0f
        }
    }

    Box {
        var size by remember { mutableStateOf(IntSize.Zero) }
        Surface(
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier
                .pointerInteraction(pointerInteractionStateHolder)
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .onClick {
                    if (Duration.between(dismissedTimestamp, Instant.now()) > Duration.ofMillis(100)) {
                        isExpanded = true
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    event.changes.forEach { change ->
                        val scrollDelta = change.scrollDelta.y
                        var index = items.indexOf(selectedItem)
                        index = if (scrollDelta > 0) {
                            min(index + 1, items.lastIndex)
                        } else {
                            max(index - 1, 0)
                        }
                        onItemSelected(items[index])
                    }
                }
                .onSizeChanged { size = it },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(height).width(IntrinsicSize.Max),
            ) {
                Box(Modifier.weight(1f).padding(start = 7.dp)) {
                    for (item in items) { // For sizing, to ensure all items fit
                        Text(
                            text = getItemName(item),
                            style = RiftTheme.typography.bodyPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0f),
                        )
                    }
                    Text(
                        text = getItemName(selectedItem),
                        color = textColor,
                        style = RiftTheme.typography.bodyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.window_buttonglow),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer(translationY = -1f)
                            .alpha(highlightAlpha),
                    )
                    Image(
                        painter = painterResource(Res.drawable.dropdown_chevron),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .height(4.dp)
                            .alpha(chevronAlpha),
                    )
                }
            }
        }

        if (isExpanded) {
            RiftDropdownPopup(
                size = size,
                backgroundColor = backgroundColor,
                items = items,
                selectedItem = selectedItem,
                getItemName = getItemName,
                maxItems = maxItems,
                onItemSelected = onItemSelected,
                onDismissRequest = {
                    isExpanded = false
                    dismissedTimestamp = Instant.now()
                },
                fieldHeight = height,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> RiftDropdownPopup(
    size: IntSize,
    backgroundColor: Color,
    items: List<T>,
    selectedItem: T,
    getItemName: (T) -> String,
    maxItems: Int,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    fieldHeight: Dp,
) {
    val fieldHeightPixels = LocalDensity.current.run { fieldHeight.toPx().roundToInt() }
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, fieldHeightPixels - 1), // -1 to cover the bottom border
        onDismissRequest = onDismissRequest,
    ) {
        val width = with(LocalDensity.current) { size.width.toDp() }
        Box(
            modifier = Modifier
                .width(width)
                .height(IntrinsicSize.Min)
                .background(backgroundColor)
                .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown)),
        ) {
            // Borders
            Box(Modifier.padding(horizontal = 7.dp).height(1.dp).fillMaxWidth().align(Alignment.TopCenter).background(RiftTheme.colors.borderGreyDropdown))
            Box(Modifier.height(1.dp).fillMaxWidth().align(Alignment.BottomCenter).background(RiftTheme.colors.borderGreyDropdown))
            Box(Modifier.width(1.dp).fillMaxHeight().align(Alignment.CenterStart).background(RiftTheme.colors.borderGreyDropdown))
            Box(Modifier.width(1.dp).fillMaxHeight().align(Alignment.CenterEnd).background(RiftTheme.colors.borderGreyDropdown))

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                val isScrolling = items.size > maxItems
                val maxHeight = (31 * maxItems).dp
                val scrollState = rememberScrollState()
                // Content
                Column(
                    modifier = Modifier
                        .padding(1.dp)
                        .padding(bottom = 2.dp)
                        .modifyIf(isScrolling) { height(maxHeight) }
                        .modifyIf(isScrolling) { verticalScroll(scrollState) }
                        .weight(1f),
                ) {
                    val colorTransitionSpec = getDropdownTransitionSpec<Color>()
                    for (item in items) {
                        key(item) {
                            val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
                            val transition = updateTransition(pointerInteractionStateHolder.current)
                            val highlightColor by transition.animateColor(colorTransitionSpec) {
                                if (item == selectedItem) {
                                    when (it) {
                                        PointerInteractionState.Normal -> RiftTheme.colors.dropdownSelected
                                        PointerInteractionState.Hover -> RiftTheme.colors.dropdownHighlighted
                                        PointerInteractionState.Press -> RiftTheme.colors.dropdownHighlighted
                                    }
                                } else {
                                    when (it) {
                                        PointerInteractionState.Normal -> Color.Transparent
                                        PointerInteractionState.Hover -> RiftTheme.colors.dropdownHovered
                                        PointerInteractionState.Press -> RiftTheme.colors.dropdownHovered
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(31.dp)
                                    .padding(bottom = 1.dp)
                                    .fillMaxWidth()
                                    .pointerInteraction(pointerInteractionStateHolder)
                                    .background(highlightColor)
                                    .onClick {
                                        onItemSelected(item)
                                        onDismissRequest()
                                    },
                            ) {
                                Text(
                                    text = getItemName(item),
                                    color = RiftTheme.colors.textPrimary,
                                    style = RiftTheme.typography.bodyPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp),
                                )
                            }
                        }
                    }
                }
                if (isScrolling) {
                    RiftVerticalScrollbar(
                        scrollState = scrollState,
                        hasBackground = false,
                        modifier = Modifier
                            .height(maxHeight)
                            .padding(horizontal = 1.dp)
                            .padding(top = 4.dp, bottom = 2.dp),
                    )
                }
            }
        }
    }
}

private fun <T> getDropdownTransitionSpec(): @Composable Transition.Segment<PointerInteractionState>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            PointerInteractionState.Normal isTransitioningTo PointerInteractionState.Hover || PointerInteractionState.Hover isTransitioningTo PointerInteractionState.Press -> spring(stiffness = Spring.StiffnessMedium)
            else -> spring(stiffness = Spring.StiffnessLow)
        }
    }
}
