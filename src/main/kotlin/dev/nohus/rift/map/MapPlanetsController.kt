package dev.nohus.rift.map

import dev.nohus.rift.repositories.PlanetTypes
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import dev.nohus.rift.repositories.PlanetsRepository
import dev.nohus.rift.repositories.PlanetsRepository.Planet
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class MapPlanetsController(
    planetsRepository: PlanetsRepository,
    private val settings: Settings,
) {

    data class MapPlanetsState(
        val selectedTypes: List<PlanetType> = emptyList(),
        val planets: Map<Int, List<Planet>>,
    )

    private val _state = MutableStateFlow(
        MapPlanetsState(
            planets = planetsRepository.getPlanets(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val planetTypes = settings.selectedPlanetTypes.mapNotNull { typeId ->
            PlanetTypes.types.firstOrNull { it.typeId == typeId }
        }
        _state.update { it.copy(selectedTypes = planetTypes) }
    }

    fun onPlanetTypesUpdate(types: List<PlanetType>) {
        _state.update { it.copy(selectedTypes = types) }
        settings.selectedPlanetTypes = types.map { it.typeId }
    }
}
