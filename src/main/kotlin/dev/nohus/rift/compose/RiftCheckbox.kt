package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.checkbox_checked
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiftCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    pointerInteractionStateHolder: PointerInteractionStateHolder = remember { PointerInteractionStateHolder() },
    modifier: Modifier = Modifier,
) {
    val checkboxColor = RiftTheme.colors.primary
    val shape = CutCornerShape(2.dp)

    val transition = updateTransition(pointerInteractionStateHolder.current)
    val shadowColor by transition.animateColor(getTransitionSpec()) {
        if (it == PointerInteractionState.Normal) Color.Transparent else checkboxColor
    }
    val borderColor by transition.animateColor(getTransitionSpec()) {
        if (it == PointerInteractionState.Normal) RiftTheme.colors.borderGreyLight else checkboxColor
    }
    val backgroundColor by transition.animateColor(getTransitionSpec()) {
        if (it == PointerInteractionState.Press) RiftTheme.colors.backgroundWhite else Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer(renderEffect = BlurEffect(4f, 4f, edgeTreatment = TileMode.Decal))
                .border(2.dp, shadowColor, shape),
        )
        Surface(
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .size(16.dp)
                .pointerInteraction(pointerInteractionStateHolder)
                .onClick { onCheckedChange(!isChecked) },
        ) {
            if (isChecked) {
                Image(
                    painter = painterResource(Res.drawable.checkbox_checked),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(checkboxColor),
                )
            }
        }
    }
}

private fun <T> getTransitionSpec(): @Composable Transition.Segment<PointerInteractionState>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            PointerInteractionState.Normal isTransitioningTo PointerInteractionState.Hover || PointerInteractionState.Hover isTransitioningTo PointerInteractionState.Press -> spring(stiffness = Spring.StiffnessMedium)
            else -> spring(stiffness = Spring.StiffnessLow)
        }
    }
}
