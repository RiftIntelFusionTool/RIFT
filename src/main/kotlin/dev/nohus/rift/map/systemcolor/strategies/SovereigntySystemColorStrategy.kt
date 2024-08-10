package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SovereigntyColorRepository
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class SovereigntySystemColorStrategy(
    private val sovereigntyColorRepository: SovereigntyColorRepository,
    @InjectedParam private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.sovereignty != null
    }

    override fun getColor(system: Int): Color {
        val sovereignty = systemStatus[system]?.sovereignty ?: return Color.Unspecified
        if (sovereignty.factionId != null) return sovereigntyColorRepository.getFactionColor(sovereignty.factionId)
        if (sovereignty.allianceId != null) return sovereigntyColorRepository.getAllianceColor(sovereignty.allianceId)
        return Color.Unspecified
    }
}
