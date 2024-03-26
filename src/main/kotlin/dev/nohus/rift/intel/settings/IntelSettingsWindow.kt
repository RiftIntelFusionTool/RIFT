package dev.nohus.rift.intel.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun IntelSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: IntelSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Intel Reports Settings",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        IntelSettingsWindowContent(
            isUsingCompactMode = state.isUsingCompact,
            onIsUsingCompactModeChange = viewModel::onIsUsingCompactModeChange,
            isShowingReporter = state.isShowingReporter,
            onIsShowingReporterChange = viewModel::onIsShowingReporterChange,
            isShowingChannel = state.isShowingChannel,
            onIsShowingChannelChange = viewModel::onIsShowingChannelChange,
            isShowingRegion = state.isShowingRegion,
            onIsShowingRegionChange = viewModel::onIsShowingRegionChange,
            isShowingSystemDistance = state.isShowingSystemDistance,
            onIsShowingSystemDistanceChange = viewModel::onIsShowingSystemDistanceChange,
        )
    }
}

@Composable
private fun IntelSettingsWindowContent(
    isUsingCompactMode: Boolean,
    onIsUsingCompactModeChange: (Boolean) -> Unit,
    isShowingReporter: Boolean,
    onIsShowingReporterChange: (Boolean) -> Unit,
    isShowingChannel: Boolean,
    onIsShowingChannelChange: (Boolean) -> Unit,
    isShowingRegion: Boolean,
    onIsShowingRegionChange: (Boolean) -> Unit,
    isShowingSystemDistance: Boolean,
    onIsShowingSystemDistanceChange: (Boolean) -> Unit,
) {
    Column {
        SectionTitle("User interface", Modifier.padding(bottom = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftCheckboxWithLabel(
                label = "Compact mode",
                isChecked = isUsingCompactMode,
                onCheckedChange = onIsUsingCompactModeChange,
            )
        }
        SectionTitle("Shown information", Modifier.padding(vertical = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftCheckboxWithLabel(
                label = "Show reporter name",
                isChecked = isShowingReporter,
                onCheckedChange = onIsShowingReporterChange,
            )
            RiftCheckboxWithLabel(
                label = "Show channel name",
                isChecked = isShowingChannel,
                onCheckedChange = onIsShowingChannelChange,
            )
            RiftCheckboxWithLabel(
                label = "Show channel region",
                isChecked = isShowingRegion,
                onCheckedChange = onIsShowingRegionChange,
            )
            RiftCheckboxWithLabel(
                label = "Show distance on systems up to 5 jumps away",
                tooltip = "Shows number of jumps to the closest character",
                isChecked = isShowingSystemDistance,
                onCheckedChange = onIsShowingSystemDistanceChange,
            )
        }
    }
}
