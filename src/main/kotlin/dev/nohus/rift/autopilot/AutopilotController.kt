package dev.nohus.rift.autopilot

import dev.nohus.rift.characters.ActiveCharacterRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.repositories.GetRouteUseCase
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class AutopilotController(
    private val activeCharacterRepository: ActiveCharacterRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val getRouteUseCase: GetRouteUseCase,
    private val esiApi: EsiApi,
    private val settings: Settings,
) {

    data class Route(val systems: List<Int>)
    data class UpdatedRoute(val full: Route, val appended: Route)

    private val _activeRoutes = MutableStateFlow<Map<Int, Route>>(emptyMap()) // Character ID -> Route
    val activeRoutes = _activeRoutes.asStateFlow()
    private val scope = CoroutineScope(Job())

    fun addWaypoint(destinationId: Long, solarSystemId: Int) {
        addWaypoint(
            destinationId = destinationId,
            solarSystemId = solarSystemId,
            addWaypoint = true,
            useIndividualWaypoints = settings.isUsingRiftAutopilotRoute,
        )
    }

    fun setDestination(destinationId: Long, solarSystemId: Int) {
        addWaypoint(
            destinationId = destinationId,
            solarSystemId = solarSystemId,
            addWaypoint = false,
            useIndividualWaypoints = settings.isUsingRiftAutopilotRoute,
        )
    }

    fun clearRoute() {
        val activeCharacterId = activeCharacterRepository.activeCharacter.value ?: return
        val currentSystemId = characterLocationRepository.locations.value[activeCharacterId]?.solarSystemId ?: return
        addWaypoint(destinationId = currentSystemId.toLong(), solarSystemId = currentSystemId, addWaypoint = false, useIndividualWaypoints = false)
        _activeRoutes.value -= activeCharacterId
    }

    private fun addWaypoint(
        destinationId: Long,
        solarSystemId: Int,
        addWaypoint: Boolean,
        useIndividualWaypoints: Boolean,
    ) = scope.launch {
        val activeCharacterId = activeCharacterRepository.activeCharacter.value ?: return@launch
        val route = getRoute(activeCharacterId, solarSystemId, addWaypoint) ?: return@launch
        _activeRoutes.value += (activeCharacterId to route.full)

        if (useIndividualWaypoints) {
            for ((index, system) in route.appended.systems.withIndex()) {
                val result = esiApi.postUiAutopilotWaypoint(
                    destinationId = system.toLong(),
                    clearOtherWaypoints = if (index == 0) !addWaypoint else false,
                    characterId = activeCharacterId,
                )
                if (result.isFailure) {
                    logger.error { "Setting autopilot waypoint failed: $result" }
                }
                delay(100)
            }
        } else {
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

    private fun getRoute(characterId: Int, solarSystemId: Int, addWaypoint: Boolean): UpdatedRoute? {
        return if (addWaypoint && characterId in _activeRoutes.value) {
            // From tip of current route
            val currentRoute = _activeRoutes.value[characterId]?.systems?.takeIf { it.isNotEmpty() } ?: return null
            val waypointRoute = getRouteUseCase(currentRoute.last(), solarSystemId, 50, withJumpBridges = true) ?: return null
            val route = currentRoute + waypointRoute.drop(1)
            UpdatedRoute(full = Route(route), appended = Route(waypointRoute.drop(1)))
        } else {
            // New route
            val currentSystemId = characterLocationRepository.locations.value[characterId]?.solarSystemId ?: return null
            val route = getRouteUseCase(currentSystemId, solarSystemId, 50, withJumpBridges = true) ?: return null
            UpdatedRoute(full = Route(route), appended = Route(route))
        }
    }
}
