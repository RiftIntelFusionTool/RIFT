package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class ClonesSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.clones?.takeIf { it.isNotEmpty() } != null
    }

    override fun getColor(system: Int): Color {
        val hasClones = systemStatus[system]?.clones?.isNotEmpty() ?: return Color.Unspecified
        return if (hasClones) Color(0xFF70E552) else Color(0xFFBC1113)
    }
}
