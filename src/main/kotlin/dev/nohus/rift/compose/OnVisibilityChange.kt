package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun OnVisibilityChange(handler: (isVisible: Boolean) -> Unit) {
    DisposableEffect(Unit) {
        handler(true)
        onDispose { handler(false) }
    }
}
