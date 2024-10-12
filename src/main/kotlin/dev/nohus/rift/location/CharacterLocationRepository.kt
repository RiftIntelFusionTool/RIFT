package dev.nohus.rift.location

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.LocationRepository.Station
import dev.nohus.rift.location.LocationRepository.Structure
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.repositories.SolarSystemsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class CharacterLocationRepository(
    private val localCharactersRepository: LocalCharactersRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val esiApi: EsiApi,
    private val localSystemChangeController: LocalSystemChangeController,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val locationRepository: LocationRepository,
) {

    private val _locations = MutableStateFlow<Map<Int, Location>>(emptyMap())
    val locations = _locations.asStateFlow()

    private val locationExpiry = Duration.ofSeconds(20)
    private val locationExpiryOffline = Duration.ofMinutes(2)

    data class Location(
        val solarSystemId: Int,
        val regionId: Int?,
        val station: Station?,
        val structure: Structure?,
        val timestamp: Instant,
    )

    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters.collect {
                loadLocations()
            }
        }
        launch {
            while (true) {
                delay(10.seconds)
                loadLocations()
            }
        }
        launch {
            localSystemChangeController.characterSystemChanges.collect { change ->
                val currentSystem = _locations.value[change.characterId]?.solarSystemId
                if (change.systemId != currentSystem) {
                    _locations.value += change.characterId to Location(
                        solarSystemId = change.systemId,
                        regionId = solarSystemsRepository.getRegionIdBySystemId(change.systemId),
                        station = null,
                        structure = null,
                        timestamp = change.timestamp,
                    )
                    logger.debug { "Location updated from logs for character ${change.characterId}" }
                }
            }
        }
    }

    private suspend fun loadLocations() {
        val minTime = Instant.now() - locationExpiry
        val minTimeOffline = Instant.now() - locationExpiryOffline
        localCharactersRepository.characters.value
            .filter { it.isAuthenticated }
            .map { it.characterId }
            .filter { id ->
                val isMissing = id !in _locations.value.keys
                if (isMissing) return@filter true
                val timestamp = _locations.value[id]?.timestamp
                val isOnline = id in onlineCharactersRepository.onlineCharacters.value
                val isExpired = if (isOnline) {
                    timestamp?.isBefore(minTime) == true
                } else {
                    timestamp?.isBefore(minTimeOffline) == true
                }
                isExpired
            }
            .forEach { id ->
                loadLocation(id)
            }
    }

    private suspend fun loadLocation(characterId: Int) {
        when (val result = esiApi.getCharacterIdLocation(characterId)) {
            is Success -> {
                val station = locationRepository.getStation(result.data.stationId)
                val structure = locationRepository.getStructure(result.data.structureId, characterId)
                _locations.value +=
                    characterId to Location(
                        solarSystemId = result.data.solarSystemId,
                        regionId = solarSystemsRepository.getRegionIdBySystemId(result.data.solarSystemId),
                        station = station,
                        structure = structure,
                        timestamp = Instant.now(),
                    )
                logger.debug { "Location updated from ESI for character $characterId" }
            }
            is Failure -> {
                logger.error { "Failed getting location for character $characterId" }
            }
        }
    }
}
