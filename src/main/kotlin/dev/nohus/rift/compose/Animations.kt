package dev.nohus.rift.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun PointerInteractionStateHolder.animateBackgroundHover(): State<Color> {
    val target = if (isHovered) RiftTheme.colors.backgroundHovered else Color.Transparent
    return animateColorAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
    )
}

@Composable
fun PointerInteractionStateHolder.animateWindowBackgroundSecondaryHover(): State<Color> {
    val target = if (isHovered) RiftTheme.colors.windowBackgroundSecondaryHovered else RiftTheme.colors.windowBackgroundSecondary
    return animateColorAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
    )
}
