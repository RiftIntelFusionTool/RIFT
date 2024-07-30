package dev.nohus.rift.gamelogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.get
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager

@Composable
fun NonEnglishEveClientWarningWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: NonEnglishEveClientWarningViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    if (state.closeEvent.get()) {
        onCloseRequest()
    }

    RiftWindow(
        title = "Non-English EVE Client",
        icon = Res.drawable.window_warning,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        NonEnglishEveClientWarningContent(
            onDoneClick = viewModel::onDoneClick,
        )
    }
}

@Composable
private fun NonEnglishEveClientWarningContent(
    onDoneClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = """
                Your EVE client is set to a language other than English.
                Some RIFT features will not work.
            """.trimIndent(),
            style = RiftTheme.typography.bodyPrimary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Spacer(Modifier.weight(1f))
            RiftButton(
                text = "Understood",
                onClick = onDoneClick,
            )
        }
    }
}
