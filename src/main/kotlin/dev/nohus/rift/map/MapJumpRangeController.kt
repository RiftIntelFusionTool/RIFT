package dev.nohus.rift.map

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.settings.persistence.JumpRange
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.math.sqrt

@Single
class MapJumpRangeController(
    private val localCharactersRepository: LocalCharactersRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val settings: Settings,
) {

    data class MapJumpRangeState(
        val target: MapJumpRangeTarget? = null,
        val distanceLy: Double = 5.0,
        val systemDistances: Map<Int, SystemDistance> = emptyMap(),
    )

    data class SystemDistance(
        val distanceLy: Double,
        val isInJumpRange: Boolean,
    )

    sealed class MapJumpRangeTarget(open val id: Int) {
        data class System(val name: String, override val id: Int) : MapJumpRangeTarget(id)
        data class Character(val name: String, override val id: Int) : MapJumpRangeTarget(id)
    }

    private val _state = MutableStateFlow(MapJumpRangeState())
    val state = _state.asStateFlow()

    init {
        loadSettings()
    }

    suspend fun start() = coroutineScope {
        launch {
            characterLocationRepository.locations.collect {
                if (_state.value.target is MapJumpRangeTarget.Character) calculateSystemsInRange()
            }
        }
        launch {
            localCharactersRepository.characters.collect { characters ->
                val target = _state.value.target
                if (target is MapJumpRangeTarget.Character) {
                    val name = characters.find { it.characterId == target.id }?.info?.success?.name
                    if (name != null) {
                        _state.update { it.copy(target = target.copy(name = name)) }
                    }
                }
            }
        }
    }

    private fun loadSettings() {
        settings.jumpRange?.let { jumpRange ->
            val system = solarSystemsRepository.getSystem(jumpRange.fromId)
            val target = if (system != null) {
                MapJumpRangeTarget.System(system.name, jumpRange.fromId)
            } else {
                val character = localCharactersRepository.characters.value.firstOrNull { it.characterId == jumpRange.fromId }
                MapJumpRangeTarget.Character(character?.info?.success?.name ?: "${jumpRange.fromId}", jumpRange.fromId)
            }
            _state.update { it.copy(target = target, distanceLy = jumpRange.distanceLy) }
        }
        calculateSystemsInRange()
    }

    fun onTargetUpdate(target: String) {
        val system = solarSystemsRepository.getSystem(target)
        if (system != null) {
            _state.update { it.copy(target = MapJumpRangeTarget.System(system.name, system.id)) }
        } else {
            val character = localCharactersRepository.characters.value.firstOrNull { it.info.success?.name == target }
            if (character != null) {
                _state.update { it.copy(target = MapJumpRangeTarget.Character(target, character.characterId)) }
            } else {
                _state.update { it.copy(target = null) }
            }
        }
        updateSettings()
    }

    fun onRangeUpdate(distanceLy: Double) {
        _state.update { it.copy(distanceLy = distanceLy) }
        updateSettings()
    }

    private fun updateSettings() {
        val fromId = _state.value.target?.id
        val distanceLy = _state.value.distanceLy
        settings.jumpRange = if (fromId != null) {
            JumpRange(fromId, distanceLy)
        } else {
            null
        }
        calculateSystemsInRange()
    }

    private fun calculateSystemsInRange() {
        val systemId = _state.value.target?.let { target ->
            when (target) {
                is MapJumpRangeTarget.Character -> characterLocationRepository.locations.value[target.id]?.solarSystemId
                is MapJumpRangeTarget.System -> target.id
            }
        } ?: run {
            _state.update { it.copy(systemDistances = emptyMap()) }
            return
        }
        val fromSystem = solarSystemsRepository.getSystem(systemId) ?: run {
            _state.update { it.copy(systemDistances = emptyMap()) }
            return
        }
        val lightYear = 9460000000000000.0
        val maxDistance = _state.value.distanceLy
        val systemDistances = solarSystemsRepository.getSystems().associate {
            val distance = fromSystem.distanceTo(it) / lightYear
            val isInRange = distance <= maxDistance
            it.id to SystemDistance(distance, isInRange)
        }
        _state.update { it.copy(systemDistances = systemDistances) }
    }

    private fun MapSolarSystem.distanceTo(other: MapSolarSystem): Double {
        val xDiff = other.x - x
        val yDiff = other.y - y
        val zDiff = other.z - z
        return sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }
}
