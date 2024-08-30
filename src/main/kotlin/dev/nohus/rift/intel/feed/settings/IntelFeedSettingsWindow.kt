package dev.nohus.rift.intel.feed.settings

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
fun IntelFeedSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: IntelFeedSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Intel Feed Settings",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        IntelFeedSettingsWindowContent(
            isUsingCompactMode = state.isUsingCompactMode,
            onIsUsingCompactModeChange = viewModel::onIsUsingCompactModeChange,
        )
    }
}

@Composable
private fun IntelFeedSettingsWindowContent(
    isUsingCompactMode: Boolean,
    onIsUsingCompactModeChange: (Boolean) -> Unit,
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
    }
}
