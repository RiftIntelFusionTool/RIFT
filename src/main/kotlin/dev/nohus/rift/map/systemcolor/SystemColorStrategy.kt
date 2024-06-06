package dev.nohus.rift.map.systemcolor

import androidx.compose.ui.graphics.Color

abstract class SystemColorStrategy {

    private val noValueColor: Color = Color(0xFF7D7E7E)

    abstract fun hasData(system: Int): Boolean

    abstract fun getColor(system: Int): Color

    fun getActiveColor(system: Int): Color {
        if (!hasData(system)) return noValueColor
        return getColor(system)
    }

    fun getInactiveColor(system: Int): Color {
        return getActiveColor(system).copy(alpha = 0.1f)
    }
}
