package dev.nohus.rift.map.systemcolor.strategies

import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategy
import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategyPalettes
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import kotlin.math.log

class KillsSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : PercentageSystemColorStrategy(PercentageSystemColorStrategyPalettes.negative) {

    override fun hasData(system: Int): Boolean {
        val kills = getKills(system)
        return kills > 0
    }

    override fun getPercentage(system: Int): Float {
        val kills = getKills(system)
        return log(kills.toFloat(), 50f).coerceIn(0.05f, 1f)
    }

    private fun getKills(system: Int): Int {
        val shipKills = systemStatus[system]?.shipKills ?: 0
        val podKills = systemStatus[system]?.podKills ?: 0
        return shipKills + podKills
    }
}
