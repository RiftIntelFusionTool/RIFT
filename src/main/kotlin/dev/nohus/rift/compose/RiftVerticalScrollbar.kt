package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.PointerInteractionState.Hover
import dev.nohus.rift.compose.PointerInteractionState.Normal
import dev.nohus.rift.compose.PointerInteractionState.Press
import dev.nohus.rift.compose.theme.RiftTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun RiftVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    hasBackground: Boolean = true,
) {
    RiftVerticalScrollbar(
        scrollKey = scrollState.value,
        scrollbarAdapter = rememberScrollbarAdapter(scrollState),
        hasBackground = hasBackground,
        modifier = modifier,
    )
}

@Composable
fun RiftVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    hasBackground: Boolean = true,
) {
    val scrollKey = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { it.index * 100_000 + it.offset } ?: 0
    RiftVerticalScrollbar(
        scrollKey = scrollKey,
        scrollbarAdapter = rememberScrollbarAdapter(listState),
        hasBackground = hasBackground,
        modifier = modifier,
    )
}

@Composable
private fun RiftVerticalScrollbar(
    scrollKey: Int,
    scrollbarAdapter: ScrollbarAdapter,
    hasBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    var lastScrollTimestamp by remember { mutableStateOf(Instant.EPOCH) }
    LaunchedEffect(scrollKey) {
        lastScrollTimestamp = Instant.now()
    }
    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            isScrolling = Duration.between(lastScrollTimestamp, Instant.now()) < Duration.ofMillis(200)
            delay(200)
        }
    }

    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val pointerInteractionState = pointerInteractionStateHolder.current
    val scrollbarInteractionState by derivedStateOf {
        when (pointerInteractionState) {
            Normal -> if (isScrolling) ScrollbarInteractionState.Scrolling else ScrollbarInteractionState.Normal
            Hover -> if (isScrolling) ScrollbarInteractionState.Scrolling else ScrollbarInteractionState.Hover
            Press -> ScrollbarInteractionState.Press
        }
    }
    val transition = updateTransition(scrollbarInteractionState)
    val colorTransitionSpec = getTransitionSpec<Color>()
    val dpTransitionSpec = getTransitionSpec<Dp>()
    val thumbColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            ScrollbarInteractionState.Normal -> RiftTheme.colors.inactiveGray
            ScrollbarInteractionState.Hover -> RiftTheme.colors.borderPrimary
            ScrollbarInteractionState.Press -> RiftTheme.colors.borderPrimaryLight
            ScrollbarInteractionState.Scrolling -> RiftTheme.colors.borderPrimaryLight
        }
    }
    val thickness by transition.animateDp(dpTransitionSpec) {
        when (it) {
            ScrollbarInteractionState.Normal -> 2.dp
            ScrollbarInteractionState.Hover -> 4.dp
            ScrollbarInteractionState.Press -> 4.dp
            ScrollbarInteractionState.Scrolling -> 2.dp
        }
    }
    val shadowThickness by transition.animateDp(dpTransitionSpec) {
        when (it) {
            ScrollbarInteractionState.Normal -> 0.dp
            ScrollbarInteractionState.Hover -> 1.dp
            ScrollbarInteractionState.Press -> 4.dp
            ScrollbarInteractionState.Scrolling -> 1.dp
        }
    }

    val background = RiftTheme.colors.windowBackground
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .modifyIf(hasBackground) { background(background) }
            .pointerInteraction(pointerInteractionStateHolder)
            .width(8.dp),
    ) {
        val minimalHeight = 30.dp
        VerticalScrollbar(
            adapter = scrollbarAdapter,
            style = ScrollbarStyle(
                minimalHeight = minimalHeight,
                thickness = shadowThickness,
                shape = RectangleShape,
                hoverDurationMillis = 0,
                unhoverColor = thumbColor,
                hoverColor = thumbColor,
            ),
            modifier = Modifier
                .fillMaxHeight()
                .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal)),
        )
        VerticalScrollbar(
            adapter = scrollbarAdapter,
            style = ScrollbarStyle(
                minimalHeight = minimalHeight,
                thickness = thickness,
                shape = RectangleShape,
                hoverDurationMillis = 0,
                unhoverColor = thumbColor,
                hoverColor = thumbColor,
            ),
            modifier = Modifier
                .fillMaxHeight(),
        )
    }
}

private enum class ScrollbarInteractionState {
    Normal, Hover, Press, Scrolling
}

private fun <T> getTransitionSpec(): @Composable Transition.Segment<ScrollbarInteractionState>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            ScrollbarInteractionState.Normal isTransitioningTo ScrollbarInteractionState.Hover ||
                ScrollbarInteractionState.Hover isTransitioningTo ScrollbarInteractionState.Press ||
                ScrollbarInteractionState.Normal isTransitioningTo ScrollbarInteractionState.Scrolling -> spring(stiffness = Spring.StiffnessHigh)
            ScrollbarInteractionState.Scrolling isTransitioningTo ScrollbarInteractionState.Normal -> spring(stiffness = Spring.StiffnessLow)
            else -> spring(stiffness = Spring.StiffnessLow)
        }
    }
}
