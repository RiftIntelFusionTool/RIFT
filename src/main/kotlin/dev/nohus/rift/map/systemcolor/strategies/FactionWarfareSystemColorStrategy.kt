package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class FactionWarfareSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    private val caldari = 500001
    private val minmatar = 500002
    private val amarr = 500003
    private val gallente = 500004

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.factionWarfare != null
    }

    override fun getColor(system: Int): Color {
        return when (systemStatus[system]?.factionWarfare?.occupierFactionId) {
            caldari -> Color(0xFF9AD2E3)
            minmatar -> Color(0xFF9D452D)
            amarr -> Color(0xFFFFEE93)
            gallente -> Color(0xFF6DB09E)
            else -> Color.White
        }
    }
}
