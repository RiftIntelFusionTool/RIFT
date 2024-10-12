package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.PointerInteractionState.Hover
import dev.nohus.rift.compose.PointerInteractionState.Normal
import dev.nohus.rift.compose.PointerInteractionState.Press
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class ButtonCornerCut {
    BottomLeft, BottomRight, None, Both
}

enum class ButtonType {
    Primary, Secondary, Negative
}

private data class ButtonColors(
    val normalBackground: Color,
    val hoverBackground: Color,
    val pressBackground: Color,
    val normalBorder: Color,
    val hoverBorder: Color,
    val pressBorder: Color,
)

@Composable
fun RiftIconButton(
    icon: DrawableResource,
    type: ButtonType = ButtonType.Primary,
    cornerCut: ButtonCornerCut = ButtonCornerCut.BottomRight,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    RiftButton(
        content = { contentColor ->
            val painter = painterResource(icon)
            Image(
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.padding(horizontal = 8.dp).size(16.dp),
            )
        },
        type = type,
        cornerCut = cornerCut,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun RiftButton(
    text: String,
    icon: DrawableResource? = null,
    type: ButtonType = ButtonType.Primary,
    cornerCut: ButtonCornerCut = ButtonCornerCut.BottomRight,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    RiftButton(
        content = { contentColor ->
            Row {
                if (icon != null) {
                    val painter = painterResource(icon)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(contentColor),
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(16.dp),
                    )
                }
                Text(
                    text = text,
                    color = contentColor,
                    style = RiftTheme.typography.bodyPrimary.copy(shadow = Shadow(offset = Offset(0f, 0f), blurRadius = 3f)),
                    maxLines = 1,
                    modifier = Modifier
                        .padding(end = 15.dp)
                        .modifyIf(icon == null) { padding(start = 15.dp) },
                )
            }
        },
        type = type,
        cornerCut = cornerCut,
        isCompact = isCompact,
        modifier = modifier,
        onClick = onClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RiftButton(
    content: @Composable (contentColor: Color) -> Unit,
    type: ButtonType = ButtonType.Primary,
    cornerCut: ButtonCornerCut = ButtonCornerCut.BottomRight,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = when (cornerCut) {
        ButtonCornerCut.BottomLeft -> CutCornerShape(bottomStart = 9.dp)
        ButtonCornerCut.BottomRight -> CutCornerShape(bottomEnd = 9.dp)
        ButtonCornerCut.Both -> CutCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp)
        ButtonCornerCut.None -> RectangleShape
    }

    val colors = when (type) {
        ButtonType.Primary -> ButtonColors(
            normalBackground = RiftTheme.colors.backgroundPrimary,
            hoverBackground = RiftTheme.colors.backgroundPrimaryLight,
            pressBackground = RiftTheme.colors.backgroundWhite,
            normalBorder = RiftTheme.colors.borderPrimary,
            hoverBorder = RiftTheme.colors.borderPrimaryLight,
            pressBorder = RiftTheme.colors.backgroundWhite,
        )
        ButtonType.Secondary -> ButtonColors(
            normalBackground = Color.Transparent,
            hoverBackground = RiftTheme.colors.backgroundPrimaryLight,
            pressBackground = RiftTheme.colors.backgroundWhite,
            normalBorder = RiftTheme.colors.borderPrimaryDark,
            hoverBorder = RiftTheme.colors.borderPrimaryLight,
            pressBorder = RiftTheme.colors.backgroundWhite,
        )
        ButtonType.Negative -> ButtonColors(
            normalBackground = Color.Transparent,
            hoverBackground = RiftTheme.colors.backgroundError,
            pressBackground = RiftTheme.colors.backgroundWhite,
            normalBorder = RiftTheme.colors.backgroundErrorDark,
            hoverBorder = RiftTheme.colors.borderError,
            pressBorder = RiftTheme.colors.backgroundWhite,
        )
    }

    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val transition = updateTransition(pointerInteractionStateHolder.current)
    val colorTransitionSpec = getButtonTransitionSpec<Color>()
    val floatTransitionSpec = getButtonTransitionSpec<Float>()
    val backgroundColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            Normal -> colors.normalBackground
            Hover -> colors.hoverBackground
            Press -> colors.pressBackground
        }
    }
    val borderColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            Normal -> colors.normalBorder
            Hover -> colors.hoverBorder
            Press -> colors.pressBorder
        }
    }
    val contentColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            Normal -> RiftTheme.colors.textPrimary
            Hover -> RiftTheme.colors.textHighlighted
            Press -> Color(0xFF595959)
        }
    }
    val shadowColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            Normal -> Color.Transparent
            Hover -> colors.hoverBorder
            Press -> colors.pressBorder
        }
    }
    val shadowStrength by transition.animateFloat(floatTransitionSpec) {
        when (it) {
            Normal -> 0.1f
            Hover -> 1f
            Press -> 2f
        }
    }

    Box(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .height(IntrinsicSize.Max),
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInteraction(pointerInteractionStateHolder)
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .onClick(onClick = onClick),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(if (isCompact) 24.dp else 32.dp),
            ) {
                content(contentColor)
            }
        }
        ButtonShadow(shadowColor, shadowStrength, shape)
    }
}

private fun <T> getButtonTransitionSpec(): @Composable Transition.Segment<PointerInteractionState>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            Normal isTransitioningTo Hover || Hover isTransitioningTo Press -> spring(stiffness = Spring.StiffnessMedium)
            else -> spring(stiffness = Spring.StiffnessLow)
        }
    }
}

@Composable
private fun ButtonShadow(
    shadowColor: Color,
    shadowStrength: Float,
    shape: Shape,
) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal))
            .border(2.dp * shadowStrength, shadowColor, shape),
    )
}
