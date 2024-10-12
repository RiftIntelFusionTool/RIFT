package dev.nohus.rift.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.utils.withColor

@Composable
fun RiftAppName(style: TextStyle) {
    val riftText = buildAnnotatedString {
        withColor(RiftTheme.colors.textSpecialHighlighted) {
            append("R")
        }
        append("IFT ")
        withColor(RiftTheme.colors.textSpecialHighlighted) {
            append("I")
        }
        append("ntel ")
        withColor(RiftTheme.colors.textSpecialHighlighted) {
            append("F")
        }
        append("usion ")
        withColor(RiftTheme.colors.textSpecialHighlighted) {
            append("T")
        }
        append("ool")
    }
    Text(
        text = riftText,
        style = style.copy(fontWeight = FontWeight.Bold),
    )
}
