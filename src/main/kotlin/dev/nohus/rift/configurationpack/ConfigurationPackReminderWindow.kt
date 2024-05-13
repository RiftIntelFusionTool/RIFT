package dev.nohus.rift.configurationpack

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.ConfigurationPackReminderViewModel.UiState
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_info
import dev.nohus.rift.get
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager

@Composable
fun ConfigurationPackReminderWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: ConfigurationPackReminderViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    if (state.closeEvent.get()) {
        onCloseRequest()
    }

    RiftWindow(
        title = "Alliance Features",
        icon = Res.drawable.window_info,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        ConfigurationPackReminderContent(
            state = state,
            onCancelClick = viewModel::onCancelClick,
            onConfirmClick = viewModel::onConfirmClick,
            onDoneClick = viewModel::onDoneClick,
        )
    }
}

@Composable
private fun ConfigurationPackReminderContent(
    state: UiState,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    AnimatedContent(targetState = state.isSuccessful) { isSuccessful ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            if (isSuccessful) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                            append("Configuration pack for ")
                            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                                append(state.suggestedConfigurationPack?.displayName)
                            }
                            append(" enabled.\nRestart the application for the change to take effect.")
                        }
                    },
                    style = RiftTheme.typography.bodyPrimary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "Done",
                        onClick = onDoneClick,
                    )
                }
            } else {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                            append("You currently have alliances features disabled.\nWould you like to enable the configuration pack for ")
                            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                                append(state.suggestedConfigurationPack?.displayName)
                            }
                            append("?")
                        }
                    },
                    style = RiftTheme.typography.bodyPrimary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "No, thanks",
                        type = ButtonType.Secondary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = onCancelClick,
                    )
                    RiftButton(
                        text = "Confirm",
                        onClick = onConfirmClick,
                    )
                }
            }
        }
    }
}
