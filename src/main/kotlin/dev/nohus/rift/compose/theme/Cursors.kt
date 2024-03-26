package dev.nohus.rift.compose.theme

import dev.nohus.rift.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object Cursors {
    private val toolkit = Toolkit.getDefaultToolkit()

    val pointer = createCursor("files/window_cursor.png")
    val pointerInteractive = createCursor("files/window_cursor_interactive.png")
    val pointerDropdown = createCursor("files/window_cursor_dropdown.png")
    val hand = createCursor("files/window_cursor_hand.png")
    val drag = createCursor("files/window_cursor_drag.png")

    private fun createCursor(resource: String): Cursor {
        val originalImage = runBlocking { ImageIO.read(Res.readBytes(resource).inputStream()) }
        val size = toolkit.getBestCursorSize(32, 32)
        val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.drawImage(originalImage, 0, 0, size.width, size.height, null)
        graphics.dispose()
        return toolkit.createCustomCursor(image, Point(15, 14), "normal")
    }
}
