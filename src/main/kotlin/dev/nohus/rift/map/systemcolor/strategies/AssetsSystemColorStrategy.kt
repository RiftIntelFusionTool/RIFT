package dev.nohus.rift.map.systemcolor.strategies

import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import kotlin.math.log

class AssetsSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : PercentageSystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.assetCount?.takeIf { it > 0 } != null
    }

    override fun getPercentage(system: Int): Float {
        val jumps = systemStatus[system]?.assetCount ?: return 0f
        return log(jumps.toFloat(), 50f).coerceIn(0.05f, 1f)
    }
}
