package dev.nohus.rift.autopilot

import dev.nohus.rift.characters.repositories.ActiveCharacterRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.repositories.GetRouteUseCase
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
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
    private val localCharactersRepository: LocalCharactersRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val getRouteUseCase: GetRouteUseCase,
    private val esiApi: EsiApi,
    private val settings: Settings,
) {

    data class Route(val systems: List<Int>)
    data class UpdatedRoute(val full: Route, val appended: Route)

    private val _activeRoutes = MutableStateFlow<Map<Int, Route>>(emptyMap()) // Character ID -> Route
    val activeRoutes = _activeRoutes.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob())

    suspend fun start() = coroutineScope {
        launch {
            observeCharacterLocation()
        }
    }

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
        getTargetCharacters().forEach { characterId ->
            scope.launch {
                clearRoute(characterId)
            }
        }
    }

    private suspend fun clearRoute(characterId: Int) {
        val currentSystemId = characterLocationRepository.locations.value[characterId]?.solarSystemId ?: return
        addWaypoint(characterId = characterId, destinationId = currentSystemId.toLong(), solarSystemId = currentSystemId, addWaypoint = false, useIndividualWaypoints = false)
        _activeRoutes.value -= characterId
    }

    private fun addWaypoint(
        destinationId: Long,
        solarSystemId: Int,
        addWaypoint: Boolean,
        useIndividualWaypoints: Boolean,
    ) {
        getTargetCharacters().forEach { characterId ->
            scope.launch {
                addWaypoint(
                    characterId = characterId,
                    destinationId = destinationId,
                    solarSystemId = solarSystemId,
                    addWaypoint = addWaypoint,
                    useIndividualWaypoints = useIndividualWaypoints,
                )
            }
        }
    }

    private suspend fun addWaypoint(
        characterId: Int,
        destinationId: Long,
        solarSystemId: Int,
        addWaypoint: Boolean,
        useIndividualWaypoints: Boolean,
    ) {
        val route = getRoute(characterId, solarSystemId, addWaypoint) ?: return
        _activeRoutes.value += (characterId to route.full)

        if (useIndividualWaypoints) {
            for ((index, system) in route.appended.systems.withIndex()) {
                val result = esiApi.postUiAutopilotWaypoint(
                    destinationId = system.toLong(),
                    clearOtherWaypoints = if (index == 0) !addWaypoint else false,
                    characterId = characterId,
                )
                if (result.isFailure) {
                    logger.error { "Setting autopilot waypoint failed: $result" }
                }
                delay(100)
            }
            if (route.appended.systems.lastOrNull()?.toLong() != destinationId) {
                val result = esiApi.postUiAutopilotWaypoint(
                    destinationId = destinationId,
                    clearOtherWaypoints = false,
                    characterId = characterId,
                )
                if (result.isFailure) {
                    logger.error { "Setting autopilot waypoint failed: $result" }
                }
            }
        } else {
            val result = esiApi.postUiAutopilotWaypoint(
                destinationId = destinationId,
                clearOtherWaypoints = !addWaypoint,
                characterId = characterId,
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

    private suspend fun observeCharacterLocation() {
        characterLocationRepository.locations.collect { locations ->
            for ((characterId, location) in locations) {
                val currentRoute = _activeRoutes.value[characterId]?.systems ?: continue
                val index = currentRoute.indexOf(location.solarSystemId)
                when {
                    index > 0 -> {
                        val newRoute = currentRoute.drop(index)
                        _activeRoutes.value += (characterId to Route(newRoute))
                    }
                    index == 0 -> {
                        // Character still at start of route
                    }
                    else -> {
                        // Character no longer on route
                        _activeRoutes.value -= characterId
                    }
                }
            }
        }
    }

    private fun getTargetCharacters(): List<Int> {
        return if (settings.isSettingAutopilotToAll) {
            onlineCharactersRepository.onlineCharacters.value
        } else {
            activeCharacterRepository.activeCharacter.value?.let { listOf(it) } ?: emptyList()
        }
    }
}
