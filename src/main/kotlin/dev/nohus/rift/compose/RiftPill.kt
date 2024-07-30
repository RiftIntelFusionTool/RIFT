package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.settings_16px
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RiftPill(
    text: String,
    icon: DrawableResource? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onEditClick: (() -> Unit)? = null,
    onHoverChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) RiftTheme.colors.primary else RiftTheme.colors.borderGreyLight
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .background(RiftTheme.colors.backgroundPrimary, shape = shape)
            .onPointerEvent(PointerEventType.Enter) { onHoverChange(true) }
            .onPointerEvent(PointerEventType.Exit) { onHoverChange(false) }
            .modifyIf(isSelected) {
                border(1.dp, borderColor, shape)
            },
    ) {
        if (onEditClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                Row(
                    modifier = Modifier
                        .onClick { onClick() }
                        .hoverBackground(
                            hoverColor = RiftTheme.colors.backgroundPrimaryLight,
                            pressColor = RiftTheme.colors.backgroundPrimaryLight,
                            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                        )
                        .padding(start = Spacing.medium, top = Spacing.small, bottom = Spacing.small, end = Spacing.small),
                ) {
                    Icon(icon)
                    Text(
                        text = text,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
                VerticalDivider(color = borderColor)
                Box(
                    modifier = Modifier
                        .onClick { onEditClick() }
                        .hoverBackground(
                            hoverColor = RiftTheme.colors.backgroundPrimaryLight,
                            pressColor = RiftTheme.colors.backgroundPrimaryLight,
                            shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                        )
                        .padding(start = Spacing.small, top = Spacing.small, bottom = Spacing.small, end = Spacing.small),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.settings_16px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .onClick { onClick() }
                    .hoverBackground(
                        hoverColor = RiftTheme.colors.backgroundPrimaryLight,
                        pressColor = RiftTheme.colors.backgroundPrimaryLight,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            ) {
                Icon(icon)
                Text(
                    text = text,
                    style = RiftTheme.typography.bodyPrimary,
                )
            }
        }
    }
}

@Composable
private fun Icon(icon: DrawableResource?) {
    if (icon != null) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(end = Spacing.small)
                .size(16.dp),
        )
    }
}
