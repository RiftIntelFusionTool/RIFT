package dev.nohus.rift.startupwarning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.get
import dev.nohus.rift.startupwarning.StartupWarningViewModel.UiState
import dev.nohus.rift.utils.toggle
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager

@Composable
fun StartupWarningWindow(
    inputModel: StartupWarningInputModel?,
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: StartupWarningViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    if (state.closeEvent.get()) {
        onCloseRequest()
    }

    val title = state.warnings.singleOrNull()?.title ?: "Warnings"
    RiftWindow(
        title = title,
        icon = Res.drawable.window_warning,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        StartupWarningContent(
            state = state,
            onDoneClick = viewModel::onDoneClick,
        )
    }
}

@Composable
private fun StartupWarningContent(
    state: UiState,
    onDoneClick: (dismissedIds: List<String>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var checkedIds by remember { mutableStateOf(listOf<String>()) }
        val showTitles = state.warnings.size > 1
        state.warnings.forEach { warning ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                if (showTitles) {
                    Text(
                        text = warning.title,
                        style = RiftTheme.typography.titleHighlighted,
                    )
                }
                Text(
                    text = warning.description,
                    style = RiftTheme.typography.bodyPrimary,
                )
                RiftCheckboxWithLabel(
                    label = "Don't show again",
                    isChecked = warning.id in checkedIds,
                    onCheckedChange = { checkedIds = checkedIds.toggle(warning.id) },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Spacer(Modifier.weight(1f))
            RiftButton(
                text = "Understood",
                onClick = { onDoneClick(checkedIds) },
            )
        }
    }
}
