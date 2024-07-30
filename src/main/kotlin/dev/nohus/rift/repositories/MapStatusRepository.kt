package dev.nohus.rift.repositories

import dev.nohus.rift.assets.AssetsRepository
import dev.nohus.rift.map.MapJumpRangeController
import dev.nohus.rift.map.MapJumpRangeController.SystemDistance
import dev.nohus.rift.map.MapPlanetsController
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.FactionWarfareSystem
import dev.nohus.rift.network.esi.Incursion
import dev.nohus.rift.network.esi.SovereigntySystem
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.Storm
import dev.nohus.rift.repositories.PlanetsRepository.Planet
import dev.nohus.rift.repositories.StationsRepository.Station
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

@Single
class MapStatusRepository(
    private val esiApi: EsiApi,
    private val assetsRepository: AssetsRepository,
    private val stationsRepository: StationsRepository,
    private val namesRepository: NamesRepository,
    private val getMetaliminalStormsUseCase: GetMetaliminalStormsUseCase,
    private val mapJumpRangeController: MapJumpRangeController,
    private val mapPlanetsController: MapPlanetsController,
) {

    private data class UniverseSystemStatus(
        val shipJumps: Int?,
        val npcKills: Int?,
        val podKills: Int?,
        val shipKills: Int?,
    )

    data class SolarSystemStatus(
        val shipJumps: Int?,
        val npcKills: Int?,
        val podKills: Int?,
        val shipKills: Int?,
        val assetCount: Int?,
        val incursion: Incursion?,
        val factionWarfare: FactionWarfareSystem?,
        val sovereignty: SovereigntySystem?,
        val stations: List<Station>,
        val storms: List<Storm>,
        val distance: SystemDistance?,
        val planets: List<Planet>,
    )

    private val universeSystemStatus = MutableStateFlow<Map<Int, UniverseSystemStatus>>(emptyMap())
    private val incursions = MutableStateFlow<Map<Int, Incursion>>(emptyMap())
    private val factionWarfare = MutableStateFlow<Map<Int, FactionWarfareSystem>>(emptyMap())
    private val sovereignty = MutableStateFlow<Map<Int, SovereigntySystem>>(emptyMap())
    private val storms = MutableStateFlow<Map<Int, List<Storm>>>(emptyMap())
    private val _status = MutableStateFlow<Map<Int, SolarSystemStatus>>(emptyMap())
    val status = _status.asStateFlow()

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                load()
                delay(5.minutes)
            }
        }
        launch {
            combine(
                universeSystemStatus,
                incursions,
                factionWarfare,
                sovereignty,
                storms,
                assetsRepository.assets,
                mapJumpRangeController.state.map { it.systemDistances },
                mapPlanetsController.state,
            ) { universe, incursions, factionWarfare, sovereignty, storms, assets, distances, planets ->
                val assetsPerSystem = getAssetCountPerSystem(assets)
                val stationsPerSystem = stationsRepository.getStations()
                val systems = (
                    universe.keys + incursions.keys + factionWarfare.keys + sovereignty.keys +
                        storms.keys + assetsPerSystem.keys + stationsPerSystem.keys + distances.keys
                    ).distinct()
                systems.associateWith { systemId ->
                    SolarSystemStatus(
                        shipJumps = universe[systemId]?.shipJumps,
                        npcKills = universe[systemId]?.npcKills,
                        podKills = universe[systemId]?.podKills,
                        shipKills = universe[systemId]?.shipKills,
                        assetCount = assetsPerSystem[systemId] ?: 0,
                        incursion = incursions[systemId],
                        factionWarfare = factionWarfare[systemId],
                        sovereignty = sovereignty[systemId],
                        stations = stationsPerSystem[systemId] ?: emptyList(),
                        storms = storms[systemId] ?: emptyList(),
                        distance = distances[systemId],
                        planets = (planets.planets[systemId] ?: emptyList()).filter { it.type in planets.selectedTypes },
                    )
                }
            }.collect {
                _status.value = it
            }
        }
    }

    private suspend fun load() {
        coroutineScope {
            launch {
                loadUniverseSystemStatus()
            }
            launch {
                loadIncursions()
            }
            launch {
                loadFactionWarfare()
            }
            launch {
                loadSovereignty()
            }
            launch {
                loadMetaliminalStorms()
            }
        }
    }

    private fun getAssetCountPerSystem(assets: AssetsRepository.Assets): Map<Int, Int> {
        return assets.list.mapNotNull {
            when (val location = it.location) {
                is AssetsRepository.AssetLocation.Station -> location.systemId
                is AssetsRepository.AssetLocation.Structure -> location.systemId
                is AssetsRepository.AssetLocation.System -> location.systemId
                is AssetsRepository.AssetLocation.AssetSafety -> null
                is AssetsRepository.AssetLocation.CustomsOffice -> null
                is AssetsRepository.AssetLocation.Unknown -> null
            }
        }.groupBy { it }.map { (systemId, assets) -> systemId to assets.size }.toMap()
    }

    private suspend fun loadUniverseSystemStatus() {
        val jumps = esiApi.getUniverseSystemJumps().success?.associateBy { it.systemId } ?: return
        val kills = esiApi.getUniverseSystemKills().success?.associateBy { it.systemId } ?: return
        val systems = (jumps.keys + kills.keys).distinct()
        universeSystemStatus.value = systems.associateWith { systemId ->
            UniverseSystemStatus(
                shipJumps = jumps[systemId]?.shipJumps,
                npcKills = kills[systemId]?.npcKills,
                podKills = kills[systemId]?.podKills,
                shipKills = kills[systemId]?.shipKills,
            )
        }
    }

    private suspend fun loadIncursions() {
        val response = esiApi.getIncursions().success ?: return
        val systems = response.flatMap { it.infestedSolarSystems }.distinct()
        incursions.value = systems.associateWith { systemId ->
            response.first { systemId in it.infestedSolarSystems }
        }
    }

    private suspend fun loadFactionWarfare() {
        val response = esiApi.getFactionWarfareSystems().success ?: return
        factionWarfare.value = response
            .also {
                val ids = it.flatMap { listOf(it.ownerFactionId, it.occupierFactionId) }
                namesRepository.resolveNames(ids)
            }
            .associateBy { it.solarSystemId }
    }

    private suspend fun loadSovereignty() {
        val response = esiApi.getSovereigntyMap().success ?: return
        sovereignty.value = response
            .filter { it.factionId != null || it.allianceId != null || it.corporationId != null }
            .also {
                val ids = it.flatMap { listOfNotNull(it.factionId, it.allianceId, it.corporationId) }
                namesRepository.resolveNames(ids)
            }
            .associateBy { it.systemId }
    }

    private suspend fun loadMetaliminalStorms() {
        storms.value = getMetaliminalStormsUseCase()
    }

    private fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        flow8: Flow<T8>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
    ): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
            args[7] as T8,
        )
    }
}
