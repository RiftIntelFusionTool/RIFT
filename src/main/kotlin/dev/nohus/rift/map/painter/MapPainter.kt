package dev.nohus.rift.map.painter

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.nohus.rift.map.DoubleOffset

interface MapPainter {

    @Composable
    fun initializeComposed()

    fun draw(
        scope: DrawScope,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        animationPercentage: Float,
    )
}
