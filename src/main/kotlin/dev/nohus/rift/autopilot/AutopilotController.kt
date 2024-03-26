package dev.nohus.rift.autopilot

import dev.nohus.rift.characters.ActiveCharacterRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.network.esi.EsiApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class AutopilotController(
    private val activeCharacterRepository: ActiveCharacterRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val esiApi: EsiApi,
) {

    private val scope = CoroutineScope(Job())

    fun addWaypoint(destinationId: Long) {
        addWaypoint(destinationId, addWaypoint = true)
    }

    fun setDestination(destinationId: Long) {
        addWaypoint(destinationId, addWaypoint = false)
    }

    fun clearRoute() {
        val activeCharacterId = activeCharacterRepository.activeCharacter.value ?: return
        val currentDestination = characterLocationRepository.locations.value[activeCharacterId]?.solarSystemId?.toLong() ?: return
        addWaypoint(destinationId = currentDestination, addWaypoint = false)
    }

    private fun addWaypoint(
        destinationId: Long,
        addWaypoint: Boolean,
    ) = scope.launch {
        val activeCharacterId = activeCharacterRepository.activeCharacter.value ?: return@launch
        val result = esiApi.postUiAutopilotWaypoint(
            destinationId = destinationId,
            clearOtherWaypoints = !addWaypoint,
            characterId = activeCharacterId,
        )
        if (result.isFailure) {
            logger.error { "Setting autopilot waypoint failed: $result" }
        }
    }
}
