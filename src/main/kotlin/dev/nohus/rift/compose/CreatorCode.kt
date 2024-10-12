package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.utils.Clipboard
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreatorCode() {
    val code = "RIFTINTEL"
    RiftTooltipArea(
        text = "Click to copy code",
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .onClick { Clipboard.copy(code) }
                .hoverBackground()
                .border(1.dp, RiftTheme.colors.borderGreyLight)
                .fillMaxWidth()
                .padding(Spacing.medium),
        ) {
            Text(
                text = "CREATOR CODE",
                style = RiftTheme.typography.headlineHighlighted.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = code,
                style = RiftTheme.typography.titlePrimary,
            )
            Row(
                modifier = Modifier.padding(top = Spacing.small),
            ) {
                Text(
                    text = "Use on the ",
                    style = RiftTheme.typography.bodyPrimary,
                )
                LinkText(
                    text = "EVE Store",
                    onClick = {
                        Clipboard.copy(code)
                        "https://store.eveonline.com".toURIOrNull()?.openBrowser()
                    },
                )
            }
        }
    }
}
