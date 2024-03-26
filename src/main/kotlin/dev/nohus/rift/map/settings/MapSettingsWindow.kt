package dev.nohus.rift.map.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.settings.persistence.IntelMap
import dev.nohus.rift.settings.persistence.MapStarColor
import dev.nohus.rift.settings.persistence.MapType
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun MapSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: MapSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Map Settings",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        MapSettingsWindowContent(
            intelMap = state.intelMap,
            onStarColorChange = viewModel::onStarColorChange,
            onIntelExpireSecondsChange = viewModel::onIntelExpireSecondsChange,
            onIntelPopupTimeoutSecondsChange = viewModel::onIntelPopupTimeoutSecondsChange,
            onIsUsingCompactModeChange = viewModel::onIsUsingCompactModeChange,
            onIsCharacterFollowingChange = viewModel::onIsCharacterFollowingChange,
            onIsScrollZoomInvertedChange = viewModel::onIsScrollZoomInvertedChange,
        )
    }
}

@Composable
private fun MapSettingsWindowContent(
    intelMap: IntelMap,
    onStarColorChange: (mapType: MapType, selected: MapStarColor) -> Unit,
    onIntelExpireSecondsChange: (Int) -> Unit,
    onIntelPopupTimeoutSecondsChange: (Int) -> Unit,
    onIsUsingCompactModeChange: (Boolean) -> Unit,
    onIsCharacterFollowingChange: (Boolean) -> Unit,
    onIsScrollZoomInvertedChange: (Boolean) -> Unit,
) {
    Column {
        SectionTitle("User interface", Modifier.padding(bottom = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftCheckboxWithLabel(
                label = "Compact mode",
                isChecked = intelMap.isUsingCompactMode,
                onCheckedChange = onIsUsingCompactModeChange,
            )
            RiftCheckboxWithLabel(
                label = "Move with character",
                tooltip = "When you jump to another system\nthe map will follow",
                isChecked = intelMap.isCharacterFollowing,
                onCheckedChange = { onIsCharacterFollowingChange(it) },
            )
            RiftCheckboxWithLabel(
                label = "Invert scroll wheel zoom",
                tooltip = "Zoom direction will be reversed",
                isChecked = intelMap.isInvertZoom,
                onCheckedChange = { onIsScrollZoomInvertedChange(it) },
            )
        }

        SectionTitle("Intel visibility", Modifier.padding(vertical = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            val expiryItems = mapOf(
                "1 minute" to 60,
                "2 minutes" to 60 * 2,
                "5 minutes" to 60 * 5,
                "15 minutes" to 60 * 15,
                "30 minutes" to 60 * 30,
                "1 hour" to 60 * 60,
                "Don't expire" to Int.MAX_VALUE,
            )
            RiftDropdownWithLabel(
                label = "Expire intel after:",
                items = expiryItems.values.toList(),
                selectedItem = intelMap.intelExpireSeconds,
                onItemSelected = onIntelExpireSecondsChange,
                getItemName = { item -> expiryItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
                tooltip = """
                    Time after a piece of intel will no longer
                    be shown on the map.
                """.trimIndent(),
            )

            val timeoutItems = mapOf(
                "10 seconds" to 10,
                "30 seconds" to 30,
                "1 minute" to 60,
                "2 minutes" to 60 * 2,
                "5 minutes" to 60 * 5,
                "15 minutes" to 60 * 15,
                "No limit" to Int.MAX_VALUE,
            )
            RiftDropdownWithLabel(
                label = "Automatically show popups for:",
                items = timeoutItems.values.toList(),
                selectedItem = intelMap.intelPopupTimeoutSeconds,
                onItemSelected = onIntelPopupTimeoutSecondsChange,
                getItemName = { item -> timeoutItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
                tooltip = """
                    For how long will intel popups be visible
                    when new information is available.
                    They are visible on hover even after this time.
                """.trimIndent(),
            )
        }

        SectionTitle("System colors", Modifier.padding(vertical = Spacing.medium))
        Row {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "New Eden map",
                    style = RiftTheme.typography.bodySecondary,
                )
                RiftRadioButtonWithLabel(
                    label = "Actual color",
                    tooltip = "Systems will be colored with\nthe actual color of the star",
                    isChecked = intelMap.mapTypeStarColor.getValue(MapType.NewEden) == MapStarColor.Actual,
                    onChecked = { onStarColorChange(MapType.NewEden, MapStarColor.Actual) },
                )
                RiftRadioButtonWithLabel(
                    label = "Security status",
                    tooltip = "Systems will be colored according\nto their security status",
                    isChecked = intelMap.mapTypeStarColor.getValue(MapType.NewEden) == MapStarColor.Security,
                    onChecked = { onStarColorChange(MapType.NewEden, MapStarColor.Security) },
                )
                RiftRadioButtonWithLabel(
                    label = "Reported hostiles",
                    tooltip = "Systems will be colored according\nto the number of reported hostiles",
                    isChecked = intelMap.mapTypeStarColor.getValue(MapType.NewEden) == MapStarColor.IntelHostiles,
                    onChecked = { onStarColorChange(MapType.NewEden, MapStarColor.IntelHostiles) },
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Region maps",
                    style = RiftTheme.typography.bodySecondary,
                )
                RiftRadioButtonWithLabel(
                    label = "Actual color",
                    tooltip = "Systems will be colored with\nthe actual color of the star",
                    isChecked = intelMap.mapTypeStarColor.getValue(MapType.Region) == MapStarColor.Actual,
                    onChecked = { onStarColorChange(MapType.Region, MapStarColor.Actual) },
                )
                RiftRadioButtonWithLabel(
                    label = "Security status",
                    tooltip = "Systems will be colored according\nto their security status",
                    isChecked = intelMap.mapTypeStarColor.getValue(MapType.Region) == MapStarColor.Security,
                    onChecked = { onStarColorChange(MapType.Region, MapStarColor.Security) },
                )
                RiftRadioButtonWithLabel(
                    label = "Reported hostiles",
                    tooltip = "Systems will be colored according\nto the number of reported hostiles",
                    isChecked = intelMap.mapTypeStarColor.getValue(MapType.Region) == MapStarColor.IntelHostiles,
                    onChecked = { onStarColorChange(MapType.Region, MapStarColor.IntelHostiles) },
                )
            }
        }
    }
}
