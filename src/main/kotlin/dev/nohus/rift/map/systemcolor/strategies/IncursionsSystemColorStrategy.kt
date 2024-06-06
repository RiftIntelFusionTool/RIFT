package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.network.esi.IncursionState
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class IncursionsSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.incursion != null
    }

    override fun getColor(system: Int): Color {
        return when (systemStatus[system]?.incursion?.state) {
            IncursionState.Withdrawing -> Color(0xFFBC1113)
            IncursionState.Mobilizing -> Color(0xFFDC6C08)
            IncursionState.Established -> Color(0xFF70E552)
            null -> Color.Unspecified
        }
    }
}
