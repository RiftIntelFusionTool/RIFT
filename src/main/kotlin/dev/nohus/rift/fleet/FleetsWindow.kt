package dev.nohus.rift.fleet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.fleet.FleetsViewModel.UiState
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_fleet
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun FleetsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: FleetsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Fleet",
        icon = Res.drawable.window_fleet,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        FleetsWindowContent(
            state = state,
            onCheckNowClick = viewModel::onCheckNowClick,
        )
    }
}

@Composable
private fun FleetsWindowContent(
    state: UiState,
    onCheckNowClick: () -> Unit,
) {
    val fleets = state.fleets
    when {
        fleets.isEmpty() -> EmptyState(onCheckNowClick)
        fleets.size == 1 -> Fleet(fleets.single())
        else -> MultipleFleets(fleets)
    }
}

@Composable
private fun EmptyState(
    onCheckNowClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "You are not in a fleet",
            style = RiftTheme.typography.titlePrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
        )
        RiftButton(
            text = "Check now",
            onClick = onCheckNowClick,
        )
    }
}

@Composable
private fun MultipleFleets(fleets: List<Fleet>) {
    Text("Multiple fleets: $fleets")
}

@Composable
private fun Fleet(fleet: Fleet) {
    Text("Fleet: $fleet")
}
