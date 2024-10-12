package dev.nohus.rift.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing

@Composable
fun RiftCheckboxWithLabel(
    label: String,
    tooltip: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val row = remember(label, isChecked) {
        movableContentOf {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = modifier,
            ) {
                RiftCheckbox(
                    isChecked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
                Text(
                    text = label,
                    style = RiftTheme.typography.bodyPrimary,
                )
            }
        }
    }
    if (tooltip != null) {
        RiftTooltipArea(
            text = tooltip,
            contentAnchor = Anchor.Left,
            horizontalOffset = 10.dp,
            content = row,
        )
    } else {
        row()
    }
}
