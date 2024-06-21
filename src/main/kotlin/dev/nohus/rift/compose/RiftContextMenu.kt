package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.Popup
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.util.UUID

private var openContextMenuId by mutableStateOf<String?>(null)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RiftContextMenuArea(
    items: List<ContextMenuItem>,
    acceptsLeftClick: Boolean = false,
    acceptsRightClick: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (items.isEmpty()) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val contextMenuId = remember { UUID.randomUUID().toString() }
    var isMenuShown by remember { mutableStateOf(false) }
    LaunchedEffect(openContextMenuId) { if (openContextMenuId != contextMenuId) isMenuShown = false }

    var areaOffset by remember { mutableStateOf(IntOffset.Zero) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .onPlaced {
                areaOffset = it.positionInRoot().let { IntOffset(it.x.toInt(), it.y.toInt()) }
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                val awtEvent = event.awtEventOrNull ?: return@onPointerEvent
                if ((acceptsRightClick && awtEvent.button == 3) || (acceptsLeftClick && awtEvent.button == 1)) {
                    with(density) {
                        offset = IntOffset(awtEvent.x.dp.roundToPx(), awtEvent.y.dp.roundToPx()) - areaOffset
                    }
                    isMenuShown = true
                    openContextMenuId = contextMenuId
                }
            },
    ) {
        content()
        if (items.isNotEmpty()) {
            RiftContextMenuPopup(
                isExpanded = isMenuShown,
                offset = offset,
                onDismissRequest = {
                    isMenuShown = false
                    openContextMenuId = null
                },
                items = items,
            )
        }
    }
}

/**
 * Manually shown version (for the map)
 */
@Composable
fun RiftContextMenuPopup(
    items: List<ContextMenuItem>,
    offset: IntOffset,
    onDismissRequest: () -> Unit,
) {
    RiftContextMenuPopup(
        isExpanded = true,
        offset = offset,
        onDismissRequest = onDismissRequest,
        items = items,
    )
}

@Composable
private fun RiftContextMenuPopup(
    isExpanded: Boolean,
    offset: IntOffset,
    onDismissRequest: () -> Unit,
    items: List<ContextMenuItem>,
) {
    if (isExpanded) {
        Popup(
            offset = offset,
            onDismissRequest = onDismissRequest,
        ) {
            Column(
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon(Cursors.pointer))
                    .width(IntrinsicSize.Max)
                    .background(RiftTheme.colors.windowBackgroundActive)
                    .border(1.dp, RiftTheme.colors.divider)
                    .padding(1.dp)
                    .padding(vertical = 7.dp),
            ) {
                val hasIconSpace = items.any { it is ContextMenuItem.TextItem && it.iconResource != null }
                for (item in items) {
                    when (item) {
                        is ContextMenuItem.TextItem -> ContextMenuRow(item.text, item.iconResource, hasIconSpace) {
                            onDismissRequest()
                            item.onClick()
                        }
                        ContextMenuItem.DividerItem -> ContextMenuDivider()
                    }
                }
            }
        }
    }
}

sealed interface ContextMenuItem {
    data class TextItem(
        val text: String,
        val iconResource: DrawableResource? = null,
        val onClick: () -> Unit,
    ) : ContextMenuItem

    data object DividerItem : ContextMenuItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContextMenuRow(
    text: String,
    iconResource: DrawableResource?,
    hasIconSpace: Boolean,
    onClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    val background by pointerState.animateBackgroundHover()
    val iconAreaSize = if (hasIconSpace) 34.dp else 0.dp
    val itemPadding = 7.dp
    Box(
        modifier = Modifier.height(21.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = itemPadding)
                .fillMaxHeight()
                .background(background)
                .pointerInteraction(pointerState)
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .onClick { onClick() },
        ) {
            Text(
                text = text,
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .padding(start = max(iconAreaSize - itemPadding, 0.dp), end = Spacing.small),
            )
        }
        if (iconResource != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.requiredSize(iconAreaSize),
            ) {
                val painter = painterResource(iconResource)
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.size(painter.intrinsicSize.height.dp),
                )
            }
        }
    }
}

@Composable
private fun ContextMenuDivider() {
    Divider(
        color = RiftTheme.colors.divider,
        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 7.dp),
    )
}
