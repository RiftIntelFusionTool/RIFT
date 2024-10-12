package dev.nohus.rift.compose.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftPill
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.di.startKoin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_info
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

private fun main() = application {
    startKoin()
    RiftTheme {
        ThemePreviewWindow(onCloseRequest = ::exitApplication)
    }
}

@Composable
private fun ThemePreviewWindow(onCloseRequest: () -> Unit) {
    RiftWindow(
        title = "RIFT – Theme Preview",
        icon = Res.drawable.window_info,
        state = RiftWindowState(windowState = rememberWindowState(width = 400.dp, height = Dp.Unspecified), isVisible = true, minimumSize = 400 to 400),
        onCloseClick = onCloseRequest,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = "Body Special Highlighted – 30%",
                style = RiftTheme.typography.bodySpecialHighlighted,
            )
            Text(
                text = "Body Highlighted – Flagship product in the Upwell Consortium's Citadel range of space stations",
                style = RiftTheme.typography.bodyHighlighted,
            )
            Text(
                text = "Body Primary – Structure Damage Limit (per second)",
                style = RiftTheme.typography.bodyPrimary,
            )
            Text(
                text = "Body Secondary – Only one Upwell Palatine Keepstar may be deployed at a time in New Eden",
                style = RiftTheme.typography.bodySecondary,
            )

            val items = listOf("All Items", "Regions", "Constellation", "Solar System").let { it + it }
            var selectedItem by remember { mutableStateOf(items.first()) }
            RiftDropdown(
                items = items,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                getItemName = { it },
            )

            var isChecked by remember { mutableStateOf(true) }
            RiftCheckboxWithLabel(
                label = if (isChecked) "Checked checkbox" else "Unchecked checkbox",
                isChecked = isChecked,
                onCheckedChange = { isChecked = it },
            )
            var isChecked2 by remember { mutableStateOf(false) }
            RiftCheckboxWithLabel(
                label = if (isChecked2) "Checked checkbox" else "Unchecked checkbox",
                isChecked = isChecked2,
                onCheckedChange = { isChecked2 = it },
            )

            var isSecondChecked by remember { mutableStateOf(false) }
            RiftRadioButtonWithLabel(
                label = "Radio button 1",
                isChecked = !isSecondChecked,
                onChecked = { isSecondChecked = false },
            )
            RiftRadioButtonWithLabel(
                label = "Radio button 2",
                isChecked = isSecondChecked,
                onChecked = { isSecondChecked = true },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                RiftButton("Jump", null, ButtonType.Primary, ButtonCornerCut.BottomLeft, false, Modifier.width(100.dp)) {}
                RiftButton("Rename", null, ButtonType.Secondary, ButtonCornerCut.None, false, Modifier.width(100.dp)) {}
                RiftButton("Destroy", null, ButtonType.Negative, ButtonCornerCut.BottomRight, false, Modifier.width(100.dp)) {}
            }

            var selectedTab by remember { mutableStateOf(2) }
            RiftTabBar(
                tabs = listOf(Tab(1, "Alliance", true), Tab(2, "Corp", true), Tab(3, "Local", true)),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onTabClosed = {},
                modifier = Modifier.border(1.dp, RiftTheme.colors.borderGreyLight),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                RiftPill("Enforcer", isSelected = true)
                RiftPill("Industrialist")
                RiftPill("Combat")
            }
        }
    }
}
