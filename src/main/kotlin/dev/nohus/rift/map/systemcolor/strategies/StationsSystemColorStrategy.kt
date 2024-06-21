package dev.nohus.rift.map.systemcolor.strategies

import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import kotlin.math.log

class StationsSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : PercentageSystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.stations?.size?.takeIf { it > 0 } != null
    }

    override fun getPercentage(system: Int): Float {
        val stations = systemStatus[system]?.stations?.size ?: return 0f
        return log(stations.toFloat(), 10f).coerceIn(0.05f, 1f)
    }
}
