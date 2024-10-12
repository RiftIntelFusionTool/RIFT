package dev.nohus.rift.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode

fun Modifier.modifyIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

fun <T> Modifier.modifyIfNotNull(element: T?, modifier: Modifier.(T) -> Modifier): Modifier {
    return if (element != null) {
        then(modifier(Modifier, element))
    } else {
        this
    }
}

fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap {
    val image = Image.makeFromBitmap(asSkiaBitmap())
    val scaled = image.scale(width, height)
    return scaled.toComposeImageBitmap()
}

fun Image.scale(width: Int, height: Int): Image {
    val bitmap = Bitmap()
    bitmap.allocN32Pixels(width, height)
    scalePixels(bitmap.peekPixels()!!, FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR), false)
    return Image.makeFromBitmap(bitmap)
}
