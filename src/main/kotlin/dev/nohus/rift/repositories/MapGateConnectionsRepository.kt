package dev.nohus.rift.repositories

import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import org.koin.core.annotation.Single

@Single
class MapGateConnectionsRepository(
    solarSystemsRepository: SolarSystemsRepository,
    starGatesRepository: StarGatesRepository,
) {

    data class GateConnection(
        val from: MapSolarSystem,
        val to: MapSolarSystem,
        val type: ConnectionType,
    )

    enum class ConnectionType {
        System, Constellation, Region
    }

    val gateConnections: List<GateConnection>
    val systemNeighbors: Map<Int, List<Int>>

    init {
        val systems = solarSystemsRepository.getSystems(knownSpace = true).associateBy { it.id }
        val deduplicatedConnections = starGatesRepository.connections.map { pair ->
            listOf(pair.first, pair.second).sorted().let { it[0] to it[1] }
        }.distinct()
        gateConnections = deduplicatedConnections.mapNotNull { (fromId, toId) ->
            val from = systems[fromId] ?: return@mapNotNull null
            val to = systems[toId] ?: return@mapNotNull null
            val type = when {
                from.regionId != to.regionId -> ConnectionType.Region
                from.constellationId != to.constellationId -> ConnectionType.Constellation
                else -> ConnectionType.System
            }
            GateConnection(
                from = from,
                to = to,
                type = type,
            )
        }
        systemNeighbors = starGatesRepository.connections
            .groupBy { it.first }
            .mapValues { (_, to) -> to.map { it.second } }
    }
}
