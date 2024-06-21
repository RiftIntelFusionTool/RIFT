package dev.nohus.rift.neocom

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.PointerInteractionState
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.getStandardTransitionSpec
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.generated.resources.window_characters
import dev.nohus.rift.generated.resources.window_chatchannels
import dev.nohus.rift.generated.resources.window_evemailtag
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.generated.resources.window_map
import dev.nohus.rift.generated.resources.window_quitgame
import dev.nohus.rift.generated.resources.window_rift_64
import dev.nohus.rift.generated.resources.window_satellite
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun NeocomWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: NeocomViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "RIFT",
        icon = Res.drawable.window_rift_64,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarStyle = TitleBarStyle.Small,
        withContentPadding = false,
        isResizable = false,
    ) {
        Column {
            NeocomButton(icon = Res.drawable.window_loudspeaker_icon, name = "Alerts", viewModel::onAlertsClick)
            NeocomButton(icon = Res.drawable.window_satellite, name = "Intel Reports", viewModel::onIntelClick)
            NeocomButton(icon = Res.drawable.window_map, name = "Intel Map", viewModel::onMapClick)
            NeocomButton(icon = Res.drawable.window_characters, name = "Characters", viewModel::onCharactersClick)
            NeocomButton(icon = Res.drawable.window_assets, name = "Assets", viewModel::onAssetsClick)
            if (state.isJabberEnabled) {
                NeocomButton(icon = Res.drawable.window_sovereignty, name = "Pings", viewModel::onPingsClick)
                NeocomButton(icon = Res.drawable.window_chatchannels, name = "Jabber", viewModel::onJabberClick)
            }
            NeocomButton(icon = Res.drawable.window_settings, name = "Settings", viewModel::onSettingsClick)
            NeocomButton(icon = Res.drawable.window_evemailtag, name = "About", viewModel::onAboutClick)
            NeocomButton(icon = Res.drawable.window_quitgame, name = "Quit", viewModel::onQuitClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NeocomButton(
    icon: DrawableResource,
    name: String,
    onClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    val colorTransitionSpec = getStandardTransitionSpec<Color>()
    val transition = updateTransition(pointerState.current)
    val textColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            PointerInteractionState.Normal -> RiftTheme.colors.textPrimary
            PointerInteractionState.Hover -> RiftTheme.colors.textHighlighted
            PointerInteractionState.Press -> RiftTheme.colors.textHighlighted
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hoverBackground()
            .fillMaxWidth()
            .pointerInteraction(pointerState)
            .onClick { onClick() },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(47.dp),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = name,
            style = RiftTheme.typography.titlePrimary.copy(color = textColor),
            modifier = Modifier.padding(end = Spacing.medium),
        )
    }
}
