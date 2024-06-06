package dev.nohus.rift.map.painter

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.nohus.rift.map.DoubleOffset
import dev.nohus.rift.map.systemcolor.SystemColorStrategy

interface MapPainter {

    @Composable
    fun initializeComposed()

    fun drawStatic(
        scope: DrawScope,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        systemColorStrategy: SystemColorStrategy,
        cellColorStrategy: SystemColorStrategy?,
    )

    fun drawAnimated(
        scope: DrawScope,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        animationPercentage: Float,
        systemColorStrategy: SystemColorStrategy,
    )
}
