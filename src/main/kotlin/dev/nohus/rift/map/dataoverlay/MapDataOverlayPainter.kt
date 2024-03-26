package dev.nohus.rift.map.dataoverlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import dev.nohus.rift.map.systemcolor.SolarSystemColorStrategy
import dev.nohus.rift.repositories.SolarSystemsRepository

abstract class MapDataOverlayPainter {
    abstract fun drawDataCircle(scope: DrawScope, system: Int)
}

abstract class PercentageMapDataOverlayPainter : MapDataOverlayPainter() {

    private val noValueColor = Color(0xFF7D7E7E)
    private val lowColor = Color(0xFF956308)
    private val highColor = Color(0xFFC30304)
    private val brushForPercentage = mutableMapOf<Float, Brush>()

    abstract fun getPercentage(system: Int): Float

    abstract fun hasData(system: Int): Boolean

    override fun drawDataCircle(scope: DrawScope, system: Int) = with(scope) {
        if (!hasData(system)) return@with

        val percentage = getPercentage(system)
        val radius = 8f + 8 * percentage
        val color = getColor(percentage)
        val brush = brushForPercentage.getOrPut(percentage) {
            Brush.radialGradient(
                listOf(color.copy(alpha = 0.7f), color.copy(alpha = 0f)),
                radius = radius,
                center = Offset.Zero,
            )
        }
        drawCircle(brush, radius = radius, center = Offset.Zero, style = Fill)
        drawCircle(color, radius = radius, center = Offset.Zero, style = Stroke(width = 1f))
    }

    private fun getColor(percentage: Float): Color {
        if (percentage == 0f) return noValueColor
        return lerp(lowColor, highColor, percentage)
    }

    fun asSolarSystemColorStrategy(): SolarSystemColorStrategy { // TODO: Caching
        return object : SolarSystemColorStrategy {
            override fun getActiveColor(system: SolarSystemsRepository.MapSolarSystem): Color {
                if (!hasData(system.id)) return noValueColor
                return getColor(getPercentage(system.id))
            }

            override fun getInactiveColor(system: SolarSystemsRepository.MapSolarSystem): Color {
                if (!hasData(system.id)) return noValueColor.copy(alpha = 0.1f)
                return getColor(getPercentage(system.id)).copy(alpha = 0.1f)
            }
        }
    }
}
