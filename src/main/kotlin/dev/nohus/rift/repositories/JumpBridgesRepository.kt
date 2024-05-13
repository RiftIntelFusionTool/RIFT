package dev.nohus.rift.repositories

import dev.nohus.rift.characters.LocalCharactersRepository
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.CharactersIdSearch
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.UniverseStructuresId
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

private const val ANSIBLEX_TYPE_ID = 35841

@Single
class JumpBridgesRepository(
    private val settings: Settings,
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    data class JumpBridgeConnection(
        val from: MapSolarSystem,
        val to: MapSolarSystem,
    )

    fun getConnections(): List<JumpBridgeConnection>? {
        return settings.jumpBridgeNetwork?.entries
            ?.mapNotNull {
                val from = solarSystemsRepository.getSystem(it.key) ?: return@mapNotNull null
                val to = solarSystemsRepository.getSystem(it.value) ?: return@mapNotNull null
                JumpBridgeConnection(from, to)
            }
            ?.takeIf { it.isNotEmpty() }
    }

    fun setConnections(connections: List<JumpBridgeConnection>) {
        settings.jumpBridgeNetwork = connections.associate { it.from.name to it.to.name }
    }

    sealed interface SearchState {
        data class Progress(val progress: Float, val connectionsCount: Int) : SearchState
        data class Result(val connections: List<JumpBridgeConnection>) : SearchState
        data object Error : SearchState
    }

    suspend fun search(): Flow<SearchState> = channelFlow {
        coroutineScope {
            val characterId = localCharactersRepository.characters.value
                .firstOrNull { it.isAuthenticated }
                ?.characterId
            if (characterId == null) {
                logger.error { "No authenticated characters" }
                send(SearchState.Error)
                return@coroutineScope
            }

            logger.info { "Searching for structures" }
            val systems = solarSystemsRepository.getSovSystems().shuffled().map { it.name }

            val foundStructureIds = mutableSetOf<Long>()
            val foundConnections = mutableListOf<Pair<String, String>>()
            val searchedSystems = mutableListOf<String>()
            val semaphore = Semaphore(10)
            systems.map { system ->
                async {
                    semaphore.withPermit {
                        var searchResult: Result<CharactersIdSearch>? = null
                        repeat(3) {
                            if (searchResult == null || searchResult?.isFailure == true) {
                                searchResult = esiApi.getCharactersIdSearch(characterId, "structure", false, search = system)
                            }
                        }
                        when (val result = searchResult) {
                            is Result.Success -> {
                                val structureIds = result.data.structure.filter { it !in foundStructureIds }
                                foundStructureIds += structureIds
                                structureIds.map { structureId ->
                                    async {
                                        var structureResult: Result<UniverseStructuresId>? = null
                                        repeat(3) {
                                            if (structureResult == null || structureResult?.isFailure == true) {
                                                structureResult = esiApi.getUniverseStructuresId(structureId, characterId)
                                            }
                                        }
                                        when (val result = structureResult) {
                                            is Result.Success -> {
                                                val ansiblexName = result.data.name
                                                    .takeIf { result.data.typeId == ANSIBLEX_TYPE_ID }
                                                if (ansiblexName != null) {
                                                    val connection = systems.filter { it in ansiblexName }
                                                        .take(2)
                                                        .sortedBy { ansiblexName.indexOf(it) }
                                                        .takeIf { it.size == 2 }
                                                        ?.let { it[0] to it[1] }
                                                    if (connection != null) {
                                                        foundConnections += connection
                                                    }
                                                }
                                            }
                                            else -> {
                                                send(SearchState.Error)
                                                cancel()
                                            }
                                        }
                                    }
                                }.awaitAll()
                            }
                            else -> {
                                send(SearchState.Error)
                                cancel()
                            }
                        }
                        searchedSystems += system
                        send(SearchState.Progress(progress = (searchedSystems.size / systems.size.toFloat()), connectionsCount = foundConnections.size))
                    }
                }
            }.awaitAll()

            val foundSystemConnections = foundConnections.mapNotNull { (from, to) ->
                val fromSystem = solarSystemsRepository.getSystem(from) ?: return@mapNotNull null
                val toSystem = solarSystemsRepository.getSystem(to) ?: return@mapNotNull null
                JumpBridgeConnection(fromSystem, toSystem)
            }
            send(SearchState.Result(foundSystemConnections))
        }
    }
}
