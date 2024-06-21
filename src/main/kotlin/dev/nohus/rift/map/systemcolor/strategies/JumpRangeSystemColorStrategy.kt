package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class JumpRangeSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.distance != null
    }

    override fun getColor(system: Int): Color {
        val distance = systemStatus[system]?.distance ?: return Color.Unspecified
        return when {
            distance.distanceLy == 0.0 -> Color(0xFF2E74DF)
            distance.isInJumpRange -> Color(0xFF70E552)
            distance.distanceLy <= 10.0 -> Color(0xFFDC6C08)
            else -> Color(0xFFBC1113)
        }
    }
}
