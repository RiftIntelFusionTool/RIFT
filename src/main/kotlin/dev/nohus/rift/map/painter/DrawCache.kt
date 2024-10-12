package dev.nohus.rift.map.painter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt

class DrawCache {

    private var cachedScale: Float = 0f

    fun updateScale(scale: Float) {
        if (cachedScale != scale) {
            cachedScale = scale
            systemConnectionLinearGradientCache.clear()
            systemCellGradientCache.clear()
            jumpBridgeStrokeCache.clear()
            jumpBridgeAutopilotStrokeCache.clear()
            jumpBridgeBrushCache.clear()
            autopilotPathEffectCache.clear()
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

    private val jumpBridgeStrokeCache = mutableMapOf<Long, Stroke>()
    fun getJumpBridgeStroke(
        width: Float,
        zoom: Float,
        density: Float,
    ): Stroke {
        val key = hash(
            width.toRawBits().toLong(),
            zoom.toRawBits().toLong(),
            density.toRawBits().toLong(),
        )
        return jumpBridgeStrokeCache.getOrPut(key) {
            Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f * zoom * density, 5f * zoom * density)))
        }
    }

    private val jumpBridgeAutopilotStrokeCache = mutableMapOf<Long, Pair<Stroke, Stroke>>()
    fun getJumpBridgeAutopilotStrokes(
        width: Float,
        density: Float,
        autopilotPathEffect: PathEffect,
    ): Pair<Stroke, Stroke> {
        val key = hash(
            width.toRawBits().toLong(),
            density.toRawBits().toLong(),
            autopilotPathEffect.hashCode().toLong(),
        )
        return jumpBridgeAutopilotStrokeCache.getOrPut(key) {
            val stroke1 = Stroke(width * 2 * density)
            val stroke2 = Stroke(width * 2 * density, pathEffect = autopilotPathEffect)
            stroke1 to stroke2
        }
    }

    private val jumpBridgeBrushCache = mutableMapOf<Long, Brush>()
    fun getJumpBridgeBrush(
        colors: List<Color>,
        start: Offset,
        end: Offset,
    ): Brush {
        val key = hash(
            colors.hashCode().toLong(),
            start.hashCode().toLong(),
            end.hashCode().toLong(),
        )
        return jumpBridgeBrushCache.getOrPut(key) {
            Brush.linearGradient(
                colors = colors,
                start = start,
                end = end,
            )
        }
    }

    private val autopilotPathEffectCache = mutableMapOf<Long, PathEffect>()
    fun getAutopilotPathEffect(
        zoom: Float,
        density: Float,
        phase: Float,
    ): PathEffect {
        val on = 16f
        val off = 8f
        val maxPhase = (on + off) * zoom * density
        val roundedPhase = ((phase % maxPhase) * 10).roundToInt() / 10f
        val key = hash(
            zoom.toRawBits().toLong(),
            density.toRawBits().toLong(),
            roundedPhase.toRawBits().toLong(),
        )
        return autopilotPathEffectCache.getOrPut(key) {
            PathEffect.dashPathEffect(floatArrayOf(on * zoom * density, off * zoom * density), roundedPhase * density)
        }
    }

    private val colorListCache = mutableMapOf<Long, List<Color>>()
    fun getColorList(
        color1: Color,
        color2: Color,
        color3: Color,
        color4: Color,
    ): List<Color> {
        val key = hash(
            color1.value.toLong(),
            color2.value.toLong(),
            color3.value.toLong(),
            color4.value.toLong(),
        )
        return colorListCache.getOrPut(key) {
            listOf(color1, color2, color3, color4)
        }
    }

    private fun hash(a: Long, b: Long): Long {
        return cantor(a, b)
    }

    private fun hash(a: Long, b: Long, c: Long): Long {
        return cantor(a, cantor(b, c))
    }

    private fun hash(a: Long, b: Long, c: Long, d: Long): Long {
        return cantor(a, cantor(b, cantor(c, d)))
    }

    private fun cantor(a: Long, b: Long): Long {
        return (a + b + 1) * (a + b) / 2 + b
    }
}
