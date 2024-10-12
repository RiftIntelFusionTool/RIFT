package dev.nohus.rift.planetaryindustry.models

import dev.nohus.rift.network.esi.PlanetType
import dev.nohus.rift.repositories.PlanetsRepository.Planet
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository.Type
import java.time.Instant

data class Colony(
    val id: String,
    val checkpointSimTime: Instant,
    val currentSimTime: Instant,
    val characterId: Int,
    val planet: Planet,
    val type: PlanetType,
    val system: MapSolarSystem,
    val upgradeLevel: Int,
    val usage: Usage,
    val links: List<Link>,
    val pins: List<Pin>,
    val routes: List<Route>,
    val status: ColonyStatus,
) {
    fun clone(): Colony {
        return copy(
            pins = pins.map { pin ->
                val contents = pin.contents.toMutableMap()
                when (pin) {
                    is Pin.Extractor -> pin.copy(contents = contents)
                    is Pin.Factory -> pin.copy(contents = contents)
                    is Pin.Storage -> pin.copy(contents = contents)
                    is Pin.Launchpad -> pin.copy(contents = contents)
                    is Pin.CommandCenter -> pin.copy(contents = contents)
                }
            },
        )
    }
}

data class Route(
    val type: Type,
    val sourcePinId: Long,
    val destinationPinId: Long,
    val quantity: Long,
    val routeId: Long,
    val waypoints: List<Long>? = null,
)

enum class RoutedState {
    Routed, InputNotRouted, OutputNotRouted
}

data class Link(
    val sourcePinId: Long,
    val destinationPinId: Long,
    val level: Int,
)

data class Usage(
    val cpuUsage: Int,
    val cpuSupply: Int,
    val powerUsage: Int,
    val powerSupply: Int,
)
