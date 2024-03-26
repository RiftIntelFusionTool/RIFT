package dev.nohus.rift.sso

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.completedicon
import dev.nohus.rift.generated.resources.doneglow
import dev.nohus.rift.generated.resources.purchase_fail_fg
import dev.nohus.rift.generated.resources.window_browser
import dev.nohus.rift.sso.SsoViewModel.UiState
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource

@Composable
fun WindowScope.SsoDialog(
    inputModel: SsoAuthority,
    parentWindowState: RiftWindowState,
    onDismiss: () -> Unit,
) {
    val viewModel: SsoViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onWindowOpened()
    }

    val title = when (inputModel) {
        SsoAuthority.Eve -> "Log in with EVE Online"
    }
    RiftDialog(
        title = title,
        icon = Res.drawable.window_browser,
        parentState = parentWindowState,
        state = rememberWindowState(width = 320.dp, height = Dp.Unspecified),
        onCloseClick = {
            viewModel.onCloseRequest()
            onDismiss()
        },
    ) {
        SsoDialogContent(
            state = state,
            onButtonClick = {
                viewModel.onCloseRequest()
                onDismiss()
            },
        )
    }
}

@Composable
private fun SsoDialogContent(
    state: UiState,
    onButtonClick: () -> Unit,
) {
    AnimatedContent(state.status) { status ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.large),
        ) {
            when (status) {
                SsoViewModel.SsoStatus.Waiting -> {
                    LoadingSpinner()
                    Text(
                        text = "Please continue in your browser",
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier
                            .padding(vertical = Spacing.large),
                    )
                    RiftButton(
                        text = "Cancel",
                        cornerCut = ButtonCornerCut.Both,
                        onClick = onButtonClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }

                SsoViewModel.SsoStatus.Complete -> {
                    SuccessIcon()
                    Text(
                        text = "Authentication successful!",
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier
                            .padding(vertical = Spacing.large),
                    )
                    RiftButton(
                        text = "OK",
                        cornerCut = ButtonCornerCut.Both,
                        onClick = onButtonClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }

                SsoViewModel.SsoStatus.Failed -> {
                    FailIcon()
                    Text(
                        text = "Authentication failed",
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier
                            .padding(vertical = Spacing.large),
                    )
                    RiftButton(
                        text = "Cancel",
                        cornerCut = ButtonCornerCut.Both,
                        onClick = onButtonClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FailIcon() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.doneglow),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Image(
            painter = painterResource(Res.drawable.purchase_fail_fg),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
    }
}

@Composable
private fun SuccessIcon() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.doneglow),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Image(
            painter = painterResource(Res.drawable.completedicon),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
    }
}
