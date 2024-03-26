package dev.nohus.rift.compose.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun RiftTheme(
    content: @Composable () -> Unit,
) {
    val colors = getRiftColors()
    val typography = getRiftTypography(colors)
    CompositionLocalProvider(
        LocalRiftColors provides colors,
        LocalRiftTypography provides typography,
        LocalTextStyle provides typography.bodyPrimary,
        LocalTextSelectionColors provides TextSelectionColors(colors.selectionHandle, colors.selectionBackground),
        content = content,
    )
}

object RiftTheme {
    val colors: RiftColors
        @Composable
        get() = LocalRiftColors.current
    val typography: RiftTypography
        @Composable
        get() = LocalRiftTypography.current
}
