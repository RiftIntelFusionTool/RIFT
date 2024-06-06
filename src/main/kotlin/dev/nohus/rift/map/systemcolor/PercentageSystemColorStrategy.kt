package dev.nohus.rift.map.systemcolor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

abstract class PercentageSystemColorStrategy(
    private val colors: List<Color> = PercentageSystemColorStrategyPalettes.full,
) : SystemColorStrategy() {

    abstract fun getPercentage(system: Int): Float

    override fun getColor(system: Int): Color {
        return getColor(getPercentage(system))
    }

    private fun getColor(percentage: Float): Color {
        val perColor = 1f / colors.size
        val index = (percentage / perColor).toInt()
        val colorFrom = colors[index.coerceAtMost(colors.lastIndex)]
        val colorTo = colors[(index + 1).coerceAtMost(colors.lastIndex)]
        val fraction = (percentage % perColor) / perColor
        return lerp(colorFrom, colorTo, fraction)
    }
}

object PercentageSystemColorStrategyPalettes {
    val full = listOf(
        Color(0xFF2E74DF),
        Color(0xFF379CF6),
        Color(0xFF4ACFF3),
        Color(0xFF5CDCA6),
        Color(0xFF70E552),
        Color(0xFFEEFF83),
        Color(0xFFDC6C08),
        Color(0xFFCE4611),
        Color(0xFFBC1113),
    )

    val negative = listOf(
        Color(0xFFD5860C),
        Color(0xFFFF0B0C),
    )
}
