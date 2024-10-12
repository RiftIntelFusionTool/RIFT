package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun TitledText(title: String, text: String) {
    Column {
        Text(
            text = title,
            style = RiftTheme.typography.bodyHighlighted,
        )
        Text(
            text = text,
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}
