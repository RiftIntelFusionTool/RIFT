package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalWindowInfo
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.windowing.LocalRiftWindowState
import java.time.Duration
import java.time.Instant

class PointerInteractionStateHolder {
    var isHovered by mutableStateOf(false)
    var isPressed by mutableStateOf(false)
    val current
        @Composable get() = when {
            isPressed -> PointerInteractionState.Press
            isHovered -> PointerInteractionState.Hover
            else -> PointerInteractionState.Normal
        }
}

enum class PointerInteractionState {
    Normal, Hover, Press
}

private val IGNORE_HOVER_ON_LOST_FOCUS_DURATION = Duration.ofMillis(100)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.pointerInteraction(state: PointerInteractionStateHolder): Modifier {
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    val windowOpenTimestamp = LocalRiftWindowState.current?.openTimestamp
    var windowLostFocusTimestamp by remember { mutableStateOf(Instant.EPOCH) }
    LaunchedEffect(isWindowFocused) {
        if (!isWindowFocused) windowLostFocusTimestamp = Instant.now()
    }
    LaunchedEffect(windowOpenTimestamp) {
        // Window was just opened, clear hover and press
        state.isHovered = false
        state.isPressed = false
    }
    return this
        .onPointerEvent(PointerEventType.Enter) {
            if (Duration.between(windowLostFocusTimestamp, Instant.now()) > IGNORE_HOVER_ON_LOST_FOCUS_DURATION) {
                state.isHovered = true
            }
        }
        .onPointerEvent(PointerEventType.Exit) { state.isHovered = false }
        .onPointerEvent(PointerEventType.Press) { state.isPressed = true }
        .onPointerEvent(PointerEventType.Release) { state.isPressed = false }
}

fun <T> getStandardTransitionSpec(): @Composable Transition.Segment<PointerInteractionState>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            PointerInteractionState.Normal isTransitioningTo PointerInteractionState.Hover || PointerInteractionState.Hover isTransitioningTo PointerInteractionState.Press -> spring(stiffness = Spring.StiffnessMedium)
            else -> spring(stiffness = Spring.StiffnessLow)
        }
    }
}

/**
 * Adds an animated hover/press background
 */
fun Modifier.hoverBackground(): Modifier = composed {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val colorTransitionSpec = getStandardTransitionSpec<Color>()
    val floatTransitionSpec = getStandardTransitionSpec<Float>()
    val transition = updateTransition(pointerInteractionStateHolder.current)
    val highlightColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            PointerInteractionState.Normal -> RiftTheme.colors.backgroundHovered
            PointerInteractionState.Hover -> RiftTheme.colors.backgroundHovered
            PointerInteractionState.Press -> RiftTheme.colors.backgroundSelected
        }
    }
    val highlightAlpha by transition.animateFloat(floatTransitionSpec) {
        when (it) {
            PointerInteractionState.Normal -> 0f
            PointerInteractionState.Hover -> 1f
            PointerInteractionState.Press -> 1f
        }
    }
    return@composed this
        .pointerInteraction(pointerInteractionStateHolder)
        .background(highlightColor.copy(alpha = highlightAlpha.coerceIn(0f..1f)))
}
