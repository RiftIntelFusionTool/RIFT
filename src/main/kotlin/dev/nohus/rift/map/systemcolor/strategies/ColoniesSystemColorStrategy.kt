package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class ColoniesSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.colonies?.takeIf { it > 0 } != null
    }

    override fun getColor(system: Int): Color {
        val hasColonies = systemStatus[system]?.colonies?.let { it > 0 } ?: return Color.Unspecified
        return if (hasColonies) Color(0xFF70E552) else Color(0xFFBC1113)
    }
}
