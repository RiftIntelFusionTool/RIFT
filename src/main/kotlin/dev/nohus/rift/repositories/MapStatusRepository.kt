package dev.nohus.rift.repositories

import dev.nohus.rift.assets.AssetsRepository
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.FactionWarfareSystem
import dev.nohus.rift.network.esi.Incursion
import dev.nohus.rift.network.esi.SovereigntySystem
import dev.nohus.rift.repositories.StationsRepository.Station
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

@Single
class MapStatusRepository(
    private val esiApi: EsiApi,
    private val assetsRepository: AssetsRepository,
    private val stationsRepository: StationsRepository,
    private val namesRepository: NamesRepository,
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
    )

    private val universeSystemStatus = MutableStateFlow<Map<Int, UniverseSystemStatus>>(emptyMap())
    private val incursions = MutableStateFlow<Map<Int, Incursion>>(emptyMap())
    private val factionWarfare = MutableStateFlow<Map<Int, FactionWarfareSystem>>(emptyMap())
    private val sovereignty = MutableStateFlow<Map<Int, SovereigntySystem>>(emptyMap())
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
                assetsRepository.assets,
            ) { universe, incursions, factionWarfare, sovereignty, assets ->
                val assetsPerSystem = getAssetCountPerSystem(assets)
                val stationsPerSystem = stationsRepository.getStations()
                val systems = (
                    universe.keys + incursions.keys + factionWarfare.keys + sovereignty.keys +
                        assetsPerSystem.keys + stationsPerSystem.keys
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
}
