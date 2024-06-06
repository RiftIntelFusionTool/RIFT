package dev.nohus.rift.map.painter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

class DrawCache {

    private var cachedScale: Float = 0f

    fun updateScale(scale: Float) {
        if (cachedScale != scale) {
            cachedScale = scale
            systemConnectionLinearGradientCache.clear()
            systemCellGradientCache.clear()
        }
    }

    private val systemConnectionLinearGradientCache = mutableMapOf<Long, Brush>()
    fun getSystemConnectionLinearGradient(
        fromColor: Color,
        toColor: Color,
        deltaOffset: Offset,
    ): Brush {
        val key = hash(fromColor.value.toLong(), toColor.value.toLong(), deltaOffset.x.toLong(), deltaOffset.y.toLong())
        return systemConnectionLinearGradientCache.getOrPut(key) {
            Brush.linearGradient(
                listOf(fromColor, toColor),
                start = Offset.Zero,
                end = deltaOffset,
            )
        }
    }

    private val systemRadialGradientCache = mutableMapOf<Long, Brush>()
    fun getSystemRadialGradient(
        systemColor: Color,
        radius: Float,
    ): Brush {
        val key = hash(systemColor.value.toLong(), radius.toRawBits().toLong())
        return systemRadialGradientCache.getOrPut(key) {
            Brush.radialGradient(
                listOf(systemColor.copy(alpha = 0.7f), systemColor.copy(alpha = 0f)),
                radius = radius,
                center = Offset.Zero,
            )
        }
    }

    private val systemCellGradientCache = mutableMapOf<Long, Brush>()
    fun getSystemCellGradient(
        cellColor: Color,
        nodeSafeZoneFraction: Float,
        cellGradientRadius: Float,
        alphaModifier: Float,
        scale: Float,
        density: Float,
    ): Brush {
        val key = hash(
            cellColor.value.toLong(),
            nodeSafeZoneFraction.toRawBits().toLong(),
            cellGradientRadius.toRawBits().toLong(),
            alphaModifier.toRawBits().toLong(),
        )
        return systemCellGradientCache.getOrPut(key) {
            Brush.radialGradient(
                0f to cellColor.copy(alpha = 0.0f),
                nodeSafeZoneFraction to cellColor.copy(alpha = 0.0f),
                (nodeSafeZoneFraction + 0.1f) to cellColor.copy(alpha = 0.5f * alphaModifier),
                0.5f to cellColor.copy(alpha = 0.5f * alphaModifier),
                1f to cellColor.copy(alpha = 0.0f),
                radius = cellGradientRadius / scale,
                center = Offset.Zero,
            )
        }
    }

    private fun hash(a: Long, b: Long): Long {
        return cantor(a, b)
    }

    private fun hash(a: Long, b: Long, c: Long, d: Long): Long {
        return cantor(a, cantor(b, cantor(c, d)))
    }

    private fun cantor(a: Long, b: Long): Long {
        return (a + b + 1) * (a + b) / 2 + b
    }
}
