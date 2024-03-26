package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.dropdown_chevron
import dev.nohus.rift.generated.resources.menu_close
import dev.nohus.rift.generated.resources.window_buttonglow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class Tab(
    val id: Int,
    val title: String,
    val isCloseable: Boolean,
    val icon: DrawableResource? = null,
    val isNotified: Boolean = false,
    val payload: Any? = null,
)

@Composable
fun RiftTabBar(
    tabs: List<Tab>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    withUnderline: Boolean = true,
    isShowingIcons: Boolean = false,
    fixedHeight: Dp? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        SubcomposeLayout { constraints ->
            val intrinsicSizePlaceableTabs = subcompose("intrinsicTabs") {
                tabs.forEach { tab ->
                    TabBarTab(
                        isSelected = selectedTab == tab.id,
                        isNotified = tab.isNotified,
                        tab = tab,
                        isShowingIcons = isShowingIcons,
                        fixedHeight = fixedHeight,
                        onTabSelected = onTabSelected,
                        onTabClosed = onTabClosed,
                        modifier = Modifier.width(IntrinsicSize.Max),
                    )
                }
            }.map { it.measure(constraints) }
            val tabsIntrinsicWidth = intrinsicSizePlaceableTabs.sumOf { it.width }

            val height = intrinsicSizePlaceableTabs.maxOf { it.height }
            layout(constraints.maxWidth, height) {
                if (tabsIntrinsicWidth <= constraints.maxWidth) { // All tabs fit at natural size
                    var x = 0
                    intrinsicSizePlaceableTabs.forEach { placeable ->
                        placeable.placeRelative(x, 0)
                        x += placeable.width
                    }
                } else { // Tabs have to overflow
                    val overflowWidth = 30.dp
                    val widthForTabs = constraints.maxWidth - overflowWidth.toPx().toInt()
                    val overflowPlaceableTabs = subcompose("overflowTabs") {
                        tabs.forEach { tab ->
                            TabBarTab(
                                isSelected = selectedTab == tab.id,
                                isNotified = tab.isNotified,
                                tab = tab,
                                isShowingIcons = isShowingIcons,
                                fixedHeight = fixedHeight,
                                onTabSelected = onTabSelected,
                                onTabClosed = onTabClosed,
                                modifier = Modifier.width(IntrinsicSize.Max),
                            )
                        }
                    }.map { it.measure(constraints) }
                    var x = 0
                    var placedTabs = 0
                    for (placeable in overflowPlaceableTabs) {
                        if (x + placeable.width <= widthForTabs) {
                            placeable.placeRelative(x, 0)
                            x += placeable.width
                            placedTabs++
                        } else {
                            break
                        }
                    }
                    val overflowDropdownPlaceable = subcompose("overflowDropdown") {
                        TabOverflowDropdown(
                            tabs = tabs.drop(placedTabs),
                            onTabSelected = onTabSelected,
                        )
                    }.map { it.measure(constraints) }
                    overflowDropdownPlaceable.first().let {
                        it.placeRelative(x, (height / 2) - (it.height / 2))
                    }
                }
            }
        }

        if (withUnderline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RiftTheme.colors.borderGreyLight),
            ) {}
        }
    }
}

@Composable
private fun TabOverflowDropdown(
    tabs: List<Tab>,
    onTabSelected: (Int) -> Unit,
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val transition = updateTransition(pointerInteractionStateHolder.current)
    RiftContextMenuArea(
        items = tabs.map { tab ->
            ContextMenuItem.TextItem(tab.title, onClick = { onTabSelected(tab.id) })
        },
        acceptsLeftClick = true,
        modifier = Modifier
            .requiredWidth(30.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.Companion
                .pointerInteraction(pointerInteractionStateHolder)
                .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown))
                .size(22.dp)
                .padding(horizontal = 4.dp),
        ) {
            val highlightAlpha by transition.animateFloat {
                when (it) {
                    PointerInteractionState.Normal -> 0f
                    PointerInteractionState.Hover -> 0.3f
                    PointerInteractionState.Press -> 0.3f
                }
            }
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
                    .height(4.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabBarTab(
    isSelected: Boolean,
    isNotified: Boolean,
    tab: Tab,
    isShowingIcons: Boolean,
    fixedHeight: Dp? = null,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
    val floatWindowTransitionSpec = getActiveWindowTransitionSpec<Float>()
    val pointerInteractionStateHolder = remember(tab.id) { PointerInteractionStateHolder() }
    val colorTransitionSpec = getStandardTransitionSpec<Color>()
    val transition = updateTransition(pointerInteractionStateHolder.current)
    val textColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            PointerInteractionState.Normal -> RiftTheme.colors.textSecondary
            PointerInteractionState.Hover -> RiftTheme.colors.textHighlighted
            PointerInteractionState.Press -> RiftTheme.colors.textHighlighted
        }
    }
    val items = if (tab.isCloseable) {
        listOf(ContextMenuItem.TextItem("Close", Res.drawable.menu_close, onClick = { onTabClosed(tab.id) }))
    } else {
        emptyList()
    }
    RiftContextMenuArea(
        items = items,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .modifyIfNotNull(fixedHeight) { height(it) }
                .widthIn(min = 30.dp)
                .pointerInteraction(pointerInteractionStateHolder)
                .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown))
                .onClick { onTabSelected(tab.id) },
        ) {
            val effectiveTextColor = if (isSelected) {
                RiftTheme.colors.textHighlighted
            } else {
                textColor
            }
            if (fixedHeight != null) Spacer(Modifier.weight(1f))
            if (isShowingIcons && tab.icon != null) {
                Image(
                    painter = painterResource(tab.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = Spacing.medium)
                        .size(16.dp),
                )
            } else {
                Text(
                    text = tab.title,
                    style = RiftTheme.typography.bodyPrimary.copy(color = effectiveTextColor),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .modifyIf(fixedHeight == null) { padding(vertical = Spacing.medium) }
                        .padding(horizontal = Spacing.medium),
                )
            }
            if (fixedHeight != null) Spacer(Modifier.weight(1f))
            val underlineAlpha by animateFloatAsState(if (isSelected || isNotified) 1f else 0f)
            if (isSelected) {
                Box(
                    modifier = Modifier.alpha(underlineAlpha),
                ) {
                    val underlineColor by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
                        if (it) RiftTheme.colors.primary else RiftTheme.colors.textPrimary
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(underlineColor),
                    ) {}
                    val blurSize by activeWindowTransition.animateFloat(floatWindowTransitionSpec) {
                        if (it) 4f else 0.1f
                    }
                    Box(
                        modifier = Modifier
                            .graphicsLayer(renderEffect = BlurEffect(blurSize, blurSize, edgeTreatment = TileMode.Decal))
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(underlineColor),
                    ) {}
                }
            } else if (isNotified) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.alpha(underlineAlpha),
                ) {
                    val underlineColor = RiftTheme.colors.extendedAwayOrange
                    Box(
                        modifier = Modifier
                            .widthIn(max = 10.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(underlineColor),
                    ) {}
                    Box(
                        modifier = Modifier
                            .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal))
                            .widthIn(max = 10.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(underlineColor),
                    ) {}
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                ) {}
            }
        }
    }
}
