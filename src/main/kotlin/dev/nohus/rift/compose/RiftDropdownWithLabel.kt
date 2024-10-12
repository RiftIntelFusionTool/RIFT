package dev.nohus.rift.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing

@Composable
fun <T> RiftDropdownWithLabel(
    label: String,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    getItemName: (T) -> String,
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
    tooltip: String? = null,
    height: Dp = 32.dp,
) {
    val row = remember(label, items, selectedItem, height) {
        movableContentOf {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = modifier,
            ) {
                Text(
                    text = label,
                    style = RiftTheme.typography.bodyPrimary,
                )
                RiftDropdown(
                    items = items,
                    selectedItem = selectedItem,
                    onItemSelected = onItemSelected,
                    getItemName = getItemName,
                    height = height,
                    maxItems = maxItems,
                )
            }
        }
    }
    if (tooltip != null) {
        RiftTooltipArea(
            text = tooltip,
        ) {
            row()
        }
    } else {
        row()
    }
}
