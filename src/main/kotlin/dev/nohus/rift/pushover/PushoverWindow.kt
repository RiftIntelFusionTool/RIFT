package dev.nohus.rift.pushover

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.pushover.PushoverViewModel.UiState
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun PushoverWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: PushoverViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Mobile Notifications",
        icon = Res.drawable.window_loudspeaker_icon,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        PushoverWindowContent(
            state = state,
            onApiTokenChanged = viewModel::onApiTokenChanged,
            onUserKeyChanged = viewModel::onUserKeyChanged,
            onSendTest = viewModel::onSendTest,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }
    }
}

@Composable
fun PushoverWindowContent(
    state: UiState,
    onApiTokenChanged: (String) -> Unit,
    onUserKeyChanged: (String) -> Unit,
    onSendTest: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = buildAnnotatedString {
                withColor(RiftTheme.colors.textHighlighted) {
                    append("Pushover")
                }
                append(" is a service for sending mobile push notifications. RIFT can use it to send you notifications for alerts.")
            },
            style = RiftTheme.typography.bodyPrimary,
        )
        Text(
            text = "Setup",
            style = RiftTheme.typography.titlePrimary,
            modifier = Modifier.padding(top = Spacing.medium),
        )
        Row {
            Text(
                text = "1. Go to the ",
                style = RiftTheme.typography.bodyPrimary,
            )
            LinkText(
                text = "Pushover website",
                onClick = { "https://pushover.net".toURIOrNull()?.openBrowser() },
            )
            Text(
                text = " and signup or login.",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        Text(
            text = "2. Paste your User Key below:",
            style = RiftTheme.typography.bodyPrimary,
        )
        var userKey by remember { mutableStateOf(state.userKey) }
        RiftTextField(
            text = userKey,
            placeholder = "User Key",
            onTextChanged = {
                userKey = it
                onUserKeyChanged(it)
            },
        )
        Row {
            Text(
                text = "3. ",
                style = RiftTheme.typography.bodyPrimary,
            )
            LinkText(
                text = "Create an API Token",
                onClick = { "https://pushover.net/apps/build".toURIOrNull()?.openBrowser() },
            )
            Text(
                text = " and paste it below:",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        var apiToken by remember { mutableStateOf(state.apiToken) }
        RiftTextField(
            text = apiToken,
            placeholder = "API Token",
            onTextChanged = {
                apiToken = it
                onApiTokenChanged(it)
            },
        )
        Row {
            Text(
                text = "4. Install the ",
                style = RiftTheme.typography.bodyPrimary,
            )
            LinkText(
                text = "Android",
                onClick = { "https://pushover.net/clients/android".toURIOrNull()?.openBrowser() },
            )
            Text(
                text = " or ",
                style = RiftTheme.typography.bodyPrimary,
            )
            LinkText(
                text = "iOS",
                onClick = { "https://pushover.net/clients/ios".toURIOrNull()?.openBrowser() },
            )
            Text(
                text = " Pushover app.",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        Text(
            text = "5. All done! You can send a test notification below.",
            style = RiftTheme.typography.bodyPrimary,
        )

        AnimatedContent(
            targetState = state.isLoading,
            modifier = Modifier.align(Alignment.End).padding(top = Spacing.medium),
        ) { isLoading ->
            if (isLoading) {
                LoadingSpinner(modifier = Modifier.size(36.dp))
            } else {
                RiftButton(
                    text = "Send test notification",
                    onClick = onSendTest,
                )
            }
        }
    }
}
