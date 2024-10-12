package dev.nohus.rift.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import dev.nohus.rift.generated.resources.Res
import org.jetbrains.compose.animatedimage.Blank
import org.jetbrains.compose.animatedimage.animate
import org.jetbrains.compose.animatedimage.loadAnimatedImage

@Composable
fun AnimatedImage(
    resource: String,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = loadOrNull { loadAnimatedImage(Res.getUri(resource)) }?.animate() ?: ImageBitmap.Blank,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun <T> loadOrNull(action: suspend () -> T?): T? {
    var result: T? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        result = action()
    }
    return result
}
