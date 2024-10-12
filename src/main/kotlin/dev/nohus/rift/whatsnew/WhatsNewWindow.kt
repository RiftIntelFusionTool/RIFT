package dev.nohus.rift.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.CreatorCode
import dev.nohus.rift.compose.Patrons
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_redeem
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.whatsnew.WhatsNewViewModel.UiState
import dev.nohus.rift.whatsnew.WhatsNewViewModel.Version
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun WhatsNewWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: WhatsNewViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "What's New",
        icon = Res.drawable.window_redeem,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        WhatsNewWindowContent(
            state = state,
            onDoneClick = onCloseRequest,
        )
    }
}

@Composable
private fun WhatsNewWindowContent(
    state: UiState,
    onDoneClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        ScrollbarLazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(state.versions) {
                VersionItem(it)
            }
        }
        var size: IntSize? by remember { mutableStateOf(null) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier
                .height(LocalDensity.current.run { size?.height?.toDp() ?: 200.dp })
                .fillMaxWidth(),
        ) {
            Box(Modifier.weight(1f).onSizeChanged { size = it }) {
                CreatorCode()
            }
            Box(Modifier.weight(1f)) {
                Patrons(state.patrons, Modifier.fillMaxHeight())
            }
        }
        RiftButton(
            text = "Done",
            onClick = onDoneClick,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun VersionItem(version: Version) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Spacing.medium),
        ) {
            Divider(
                color = RiftTheme.colors.divider,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Version ${version.version}",
                style = RiftTheme.typography.headlineHighlighted.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(horizontal = Spacing.medium),
            )
            Divider(
                color = RiftTheme.colors.divider,
                modifier = Modifier.weight(1f),
            )
        }
        version.points.forEach { point ->
            Row {
                if (!point.isHighlighted) {
                    Bullet()
                    Spacer(Modifier.width(Spacing.medium))
                }
                val style = if (point.isHighlighted) {
                    RiftTheme.typography.titlePrimary.copy(
                        color = RiftTheme.colors.textSpecialHighlighted,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    RiftTheme.typography.titlePrimary
                }
                Text(
                    text = point.text,
                    style = style,
                )
            }
        }
    }
}

@Composable
private fun Bullet() {
    val color = RiftTheme.colors.textHighlighted
    val blur = 4f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .border(2.dp, color, CircleShape),
        ) {}
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        ) {}
    }
}
