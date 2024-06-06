package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapGateConnectionsRepository
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class SovereigntySystemColorStrategy(
    private val mapGateConnectionsRepository: MapGateConnectionsRepository,
    @InjectedParam private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    private val factionColors = mapOf(
        500001 to Color(0xFF9AD2E3), // Caldari State
        500002 to Color(0xFF9D452D), // Minmatar Republic
        500003 to Color(0xFFFFEE93), // Amarr Empire
        500004 to Color(0xFF6DB09E), // Gallente Federation
        500005 to Color(0xFF063C26), // Jove Empire
        500006 to Color(0xFF135433), // CONCORD Assembly
        500007 to Color(0xFF38663C), // Ammatar Mandate
        500008 to Color(0xFF80223B), // Khanid Kingdom
        500009 to Color(0xFF577440), // The Syndicate
        500010 to Color(0xFF763A38), // Guristas Pirates
        500011 to Color(0xFF718050), // Angel Cartel
        500012 to Color(0xFF7C3E2C), // Blood Raider Covenant
        500013 to Color(0xFF825F3B), // EverMore
        500014 to Color(0xFF837850), // ORE
        500015 to Color(0xFF83876A), // Thukker Tribe
        500016 to Color(0xFF063D27), // Servant Sisters of EVE
        500017 to Color(0xFF135433), // The Society of Conscious Thought
        500018 to Color(0xFF38673C), // Mordu's Legion Command
        500019 to Color(0xFF82233B), // Sansha's Nation
        500020 to Color(0xFF577440), // Serpentis
        500026 to Color(0xFF838768), // Triglavian Collective
        500029 to Color(0xFF38673C), // Deathless Circle
    )
    private val allianceColors = mutableMapOf<Int, Color>()

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.sovereignty != null
    }

    override fun getColor(system: Int): Color {
        val sovereignty = systemStatus[system]?.sovereignty ?: return Color.Unspecified
        if (sovereignty.factionId != null) return factionColors[sovereignty.factionId] ?: Color.Unspecified
        if (sovereignty.allianceId != null) {
            allianceColors[sovereignty.allianceId]?.let { return it }
            // Assign color
            val neighbors = mapGateConnectionsRepository.systemNeighbors[system] ?: emptyList()
            val neighborColors = neighbors.map {
                val neighborSovereignty = systemStatus[it]?.sovereignty
                factionColors[neighborSovereignty?.factionId] ?: allianceColors[neighborSovereignty?.allianceId]
            }
            val color = factionColors.entries
                .drop(4) // No empire colors
                .filter { it.value !in neighborColors } // No neighboring colors
                .random().value
            allianceColors[sovereignty.allianceId] = color
            return color
        }
        return Color.Unspecified
    }
}
