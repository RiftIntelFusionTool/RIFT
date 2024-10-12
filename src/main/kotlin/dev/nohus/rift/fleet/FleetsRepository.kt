package dev.nohus.rift.fleet

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import retrofit2.HttpException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class FleetsRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
) {

    private val _fleets = MutableStateFlow<Map<Long, Fleet>>(emptyMap())
    val fleets = _fleets.asStateFlow()

    // character ID -> fleet ID
    private val joinedFleets = mutableMapOf<Int, Long>()
    private val checkNowFlow = MutableSharedFlow<Unit>()
    private val checkNowMutex = Mutex()

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                updateFleets()
                delay(1.minutes)
            }
        }
        launch {
            while (true) {
                updateFleetDetails()
                delay(5.seconds)
            }
        }
        launch {
            checkNowFlow.collect {
                checkNowMutex.withLock {
                    updateFleets()
                    delay(2.seconds)
                }
            }
        }
    }

    suspend fun updateNow() {
        if (!checkNowMutex.isLocked) checkNowFlow.emit(Unit)
    }

    private suspend fun updateFleets() = coroutineScope {
        val online = onlineCharactersRepository.onlineCharacters.value
        val characters = localCharactersRepository.characters.value
            .filter { it.isAuthenticated }.map { it.characterId }.filter { it in online }
        var needsDetailsUpdate = false
        characters.map { characterId ->
            async { characterId to esiApi.getCharactersIdFleet(characterId) }
        }.awaitAll().map { (characterId, result) ->
            when (result) {
                is Success -> {
                    val currentFleet = joinedFleets[characterId]
                    if (currentFleet != result.data.id) {
                        joinedFleets[characterId] = result.data.id
                        needsDetailsUpdate = true
                        logger.info { "Character $characterId joined fleet" }
                    }
                }
                is Failure -> {
                    if (result.cause is HttpException && result.cause.code() == 404) {
                        if (characterId in joinedFleets) {
                            joinedFleets -= characterId
                            needsDetailsUpdate = true
                            logger.info { "Character $characterId left fleet" }
                        }
                    }
                }
            }
        }
        if (needsDetailsUpdate) {
            updateFleetDetails()
        }
    }

    private suspend fun updateFleetDetails() = coroutineScope {
        if (joinedFleets.isEmpty() && _fleets.value.isEmpty()) return@coroutineScope

        // Remove fleets we are no longer members of
        val fleetIds = joinedFleets.values
        _fleets.value = _fleets.value.filterKeys { it in fleetIds }

        joinedFleets.forEach { (characterId, result) ->
            launch { updateFleetDetails(characterId, result) }
        }
    }

    private suspend fun updateFleetDetails(characterId: Int, fleetId: Long) {
        val details = esiApi.getFleetsId(characterId, fleetId).success ?: return
        val members = esiApi.getFleetsIdMembers(characterId, fleetId).success ?: return
        val fleet = Fleet(
            id = fleetId,
            members = members,
            details = details,
        )
        logger.info { "Fleet: $fleet" }
        _fleets.value += (fleetId to fleet)
    }
}
