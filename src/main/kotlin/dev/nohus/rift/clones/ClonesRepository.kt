package dev.nohus.rift.clones

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.location.LocationRepository
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.LocationType
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

private val logger = KotlinLogging.logger {}

@Single
class ClonesRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val locationRepository: LocationRepository,
    private val typesRepository: TypesRepository,
    private val characterLocationRepository: CharacterLocationRepository,
) {

    private val _clones = MutableStateFlow<Map<Int, List<Clone>>>(emptyMap())
    val clones = _clones.asStateFlow()

    private var lastUpdated: Instant = Instant.EPOCH
    private var scheduledUpdate: Instant? = null
    private val dockedLocations = mutableMapOf<Int, Long?>()

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters
                .map { characters -> characters.filter { it.isAuthenticated }.map { it.characterId } }
                .distinctUntilChanged()
                .debounce(500)
                .collect {
                    updateClones()
                }
        }
        launch {
            characterLocationRepository.locations.collect { locations ->
                locations.forEach { (characterId, location) ->
                    val previous = dockedLocations[characterId]
                    val current = location.structure?.structureId ?: location.station?.stationId?.toLong()
                    if (previous != null && current != null && previous != current) {
                        logger.info { "Character $characterId jump cloned, updating clones" }
                        onNeedToUpdate()
                    } else if (previous != null && current == null) {
                        logger.info { "Character $characterId undocked, updating clones" }
                        onNeedToUpdate()
                    }
                    dockedLocations[characterId] = current
                }
            }
        }
        launch {
            while (true) {
                delay(2.minutes)
                if (Duration.between(lastUpdated, Instant.now()) > Duration.ofMinutes(15)) {
                    updateClones()
                }
            }
        }
        launch {
            while (true) {
                val scheduled = scheduledUpdate
                if (scheduled != null) {
                    scheduledUpdate = null
                    val remaining = Duration.between(Instant.now(), scheduled)
                    delay(remaining.toKotlinDuration())
                    updateClones()
                } else {
                    delay(1.minutes)
                }
            }
        }
    }

    fun onCloneJump() {
        logger.info { "Character jump cloned, scheduling clones update" }
        scheduleUpdate()
    }

    private suspend fun onNeedToUpdate() {
        // Update now, and schedule another update after the ESI cache timeout
        scheduleUpdate()
        updateClones()
    }

    private fun scheduleUpdate() {
        scheduledUpdate = Instant.now() + Duration.ofMinutes(2)
    }

    private suspend fun updateClones() {
        val characterIds = localCharactersRepository.characters.value.filter { it.isAuthenticated }.map { it.characterId }
        val clones = getClones(characterIds)
        if (clones != null) {
            _clones.value = clones
            lastUpdated = Instant.now()
            logger.info { "Updated clones" }
        } else {
            logger.error { "Could not update clones" }
        }
    }

    private suspend fun getClones(characterIds: List<Int>): Map<Int, List<Clone>>? = coroutineScope {
        val clonesDeferred = characterIds.map { characterId ->
            async { characterId to esiApi.getCharactersIdClones(characterId) }
        }
        val implantsDeferred = characterIds.map { characterId ->
            async { characterId to esiApi.getCharactersIdImplants(characterId) }
        }
        val cloneResults = clonesDeferred.awaitAll()
            .map { (characterId, result) -> characterId to (result.success ?: return@coroutineScope null) }
        val implantsResults = implantsDeferred.awaitAll()
            .map { (characterId, result) -> characterId to (result.success ?: return@coroutineScope null) }

        val clones = cloneResults.map { (characterId, clones) ->
            characterId to clones.jumpClones.map { clone ->
                Clone(
                    id = clone.id,
                    implants = getImplants(clone.implants),
                    station = if (clone.locationType == LocationType.Station) {
                        locationRepository.getStation(clone.locationId.toInt())
                    } else {
                        null
                    },
                    structure = if (clone.locationType == LocationType.Structure) {
                        locationRepository.getStructure(clone.locationId, characterId)
                    } else {
                        null
                    },
                    isActive = false,
                )
            }
        }
        val implants = implantsResults.map { (characterId, implants) ->
            characterId to listOf(
                Clone(
                    id = 0,
                    implants = getImplants(implants),
                    station = null,
                    structure = null,
                    isActive = true,
                ),
            )
        }
        val combined = (clones + implants).groupBy { it.first }.map { (characterId, groups) ->
            characterId to groups.flatMap { it.second }
        }.toMap()

        return@coroutineScope combined
    }

    private fun getImplants(implants: List<Int>): List<Type> {
        return implants.map { typeId ->
            typesRepository.getTypeOrPlaceholder(typeId)
        }
    }
}
