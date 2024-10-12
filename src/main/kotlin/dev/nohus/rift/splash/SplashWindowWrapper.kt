package dev.nohus.rift.splash

import androidx.compose.runtime.Composable

@Composable
fun SplashWindowWrapper(
    isVisible: Boolean,
    onCloseRequest: () -> Unit,
) {
    if (isVisible) {
        SplashWindow(
            onCloseRequest = onCloseRequest,
        )
    }
}
