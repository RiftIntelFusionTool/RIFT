package dev.nohus.rift.map.systemcolor.strategies

import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import kotlin.math.log

class NpcKillsSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : PercentageSystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.npcKills?.takeIf { it > 0 } != null
    }

    override fun getPercentage(system: Int): Float {
        val kills = systemStatus[system]?.npcKills ?: return 0f
        return log(kills.toFloat(), 1500f).coerceIn(0.05f, 1f)
    }
}
