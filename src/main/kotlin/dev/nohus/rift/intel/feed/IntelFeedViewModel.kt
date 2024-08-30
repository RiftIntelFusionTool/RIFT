package dev.nohus.rift.intel.feed

import dev.nohus.rift.ViewModel
import dev.nohus.rift.intel.state.CharacterBound
import dev.nohus.rift.intel.state.Clearable
import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.IntelStateController.Dated
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.map.MapExternalControl
import dev.nohus.rift.repositories.GetSystemDistanceFromCharacterUseCase
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.DistanceFilter
import dev.nohus.rift.settings.persistence.EntityFilter
import dev.nohus.rift.settings.persistence.LocationFilter
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.settings.persistence.SortingFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class IntelFeedViewModel(
    private val intelStateController: IntelStateController,
    private val characterLocationRepository: CharacterLocationRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val mapExternalControl: MapExternalControl,
    private val getSystemDistanceFromCharacterUseCase: GetSystemDistanceFromCharacterUseCase,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val intel: List<Pair<String, List<Dated<SystemEntity>>>> = emptyList(),
        val totalIntelSystems: Int = 0,
        val search: String? = null,
        val settings: IntelFeedSettings,
    )

    private val _state = MutableStateFlow(
        UiState(
            settings = getSettings(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update { it.copy(settings = getSettings()) }
                updateFilteredIntel()
            }
        }
        viewModelScope.launch {
            intelStateController.state.collect { intel ->
                _state.update {
                    it.copy(
                        intel = getFilteredIntel(intel),
                        totalIntelSystems = intel.size,
                    )
                }
            }
        }
        viewModelScope.launch {
            characterLocationRepository.locations.collect {
                if (_state.value.settings.distanceFilter != DistanceFilter.All) {
                    updateFilteredIntel()
                }
            }
        }
        viewModelScope.launch {
            mapExternalControl.openedRegion.collect {
                if (LocationFilter.CurrentMapRegion in _state.value.settings.locationFilters) {
                    updateFilteredIntel()
                }
            }
        }
    }

    private fun getSettings(): IntelFeedSettings {
        return IntelFeedSettings(
            locationFilters = settings.intelFeed.locationFilters,
            distanceFilter = settings.intelFeed.distanceFilter,
            entityFilters = settings.intelFeed.entityFilters,
            sortingFilter = settings.intelFeed.sortingFilter,
            isUsingCompactMode = settings.intelFeed.isUsingCompactMode,
            isShowingSystemDistance = settings.isShowingSystemDistance,
            isUsingJumpBridgesForDistance = settings.isUsingJumpBridgesForDistance,
        )
    }

    fun onLocationFilterSelect(selection: LocationFilter) {
        val current = _state.value.settings.locationFilters
        val new = if (selection in current) current - selection else current + selection
        _state.update { it.copy(settings = it.settings.copy(locationFilters = new)) }
        settings.intelFeed = settings.intelFeed.copy(locationFilters = new)
        updateFilteredIntel()
    }

    fun onDistanceFilterSelect(selection: DistanceFilter) {
        _state.update { it.copy(settings = it.settings.copy(distanceFilter = selection)) }
        settings.intelFeed = settings.intelFeed.copy(distanceFilter = selection)
        updateFilteredIntel()
    }

    fun onEntityFilterSelect(selection: EntityFilter) {
        val current = _state.value.settings.entityFilters
        val new = if (selection in current) current - selection else current + selection
        _state.update { it.copy(settings = it.settings.copy(entityFilters = new)) }
        settings.intelFeed = settings.intelFeed.copy(entityFilters = new)
        updateFilteredIntel()
    }

    fun onSortingFilterSelect(selection: SortingFilter) {
        _state.update { it.copy(settings = it.settings.copy(sortingFilter = selection)) }
        settings.intelFeed = settings.intelFeed.copy(sortingFilter = selection)
        updateFilteredIntel()
    }

    fun onSearchChange(text: String) {
        val search = text.takeIf { it.isNotBlank() }?.trim()
        _state.update { it.copy(search = search) }
        updateFilteredIntel()
    }

    private fun updateFilteredIntel() {
        val filteredIntel = getFilteredIntel(intelStateController.state.value)
        _state.update { it.copy(intel = filteredIntel) }
    }

    private fun getFilteredIntel(
        intel: Map<String, List<Dated<SystemEntity>>>,
    ): List<Pair<String, List<Dated<SystemEntity>>>> {
        val locationFilterRegionIds = _state.value.settings.locationFilters.flatMap { filter ->
            when (filter) {
                LocationFilter.KnownSpace -> solarSystemsRepository.getKnownSpaceRegions().map { it.id }
                LocationFilter.WormholeSpace -> solarSystemsRepository.getWormholeSpaceRegions().map { it.id }
                LocationFilter.AbyssalSpace -> solarSystemsRepository.getAbyssalSpaceRegions().map { it.id }
                LocationFilter.CurrentMapRegion -> mapExternalControl.openedRegion.value?.let { listOf(it) } ?: solarSystemsRepository.getKnownSpaceRegions().map { it.id }
            }
        }.toSet()

        var filteredSystems = when (val filter = _state.value.settings.distanceFilter) {
            DistanceFilter.All -> getFilteredIntelByRegions(intel, locationFilterRegionIds)
            DistanceFilter.CharacterLocationRegions -> {
                val regionIds = characterLocationRepository.locations.value.values.mapNotNull { it.regionId }.toSet()
                getFilteredIntelByRegions(intel, locationFilterRegionIds intersect regionIds)
            }
            is DistanceFilter.WithinDistance -> {
                getFilteredIntelByRegions(intel, locationFilterRegionIds).filterKeys { system ->
                    val systemId = solarSystemsRepository.getSystem(system)?.id ?: return@filterKeys false
                    val distance = getSystemDistanceFromCharacterUseCase(systemId, filter.jumps, withJumpBridges = _state.value.settings.isUsingJumpBridgesForDistance)
                    distance <= filter.jumps
                }
            }
        }

        val search = _state.value.search?.lowercase()
        if (search != null) {
            filteredSystems = filteredSystems
                .filter { (system, intel) -> search in system.lowercase() || intel.any { search in it.item } }
        }

        val entityFilters = _state.value.settings.entityFilters
        val filtered = filteredSystems.mapValues { (_, entities) ->
            entities.filter { datedEntity ->
                when (datedEntity.item) {
                    is SystemEntity.Killmail -> EntityFilter.Killmails in entityFilters
                    is SystemEntity.Character,
                    is SystemEntity.UnspecifiedCharacter,
                    is SystemEntity.Ship,
                    is SystemEntity.NoVisual,
                    -> EntityFilter.Characters in entityFilters
                    else -> EntityFilter.Other in entityFilters
                }
            }
        }.filterValues { it.isNotEmpty() }

        return when (_state.value.settings.sortingFilter) {
            SortingFilter.Distance -> {
                filtered.entries.sortedBy {
                    val systemId = solarSystemsRepository.getSystem(it.key)?.id ?: return@sortedBy Int.MAX_VALUE
                    getSystemDistanceFromCharacterUseCase(systemId, 9, withJumpBridges = _state.value.settings.isUsingJumpBridgesForDistance)
                }
            }
            SortingFilter.Time -> {
                filtered.entries.sortedByDescending {
                    it.value.maxOfOrNull { it.timestamp }
                }
            }
        }.map { it.key to it.value }
    }

    private fun getFilteredIntelByRegions(
        intel: Map<String, List<Dated<SystemEntity>>>,
        regionIds: Set<Int>,
    ): Map<String, List<Dated<SystemEntity>>> {
        return intel.filterKeys { system -> solarSystemsRepository.getSystem(system)?.regionId in regionIds }
    }

    private operator fun SystemEntity.contains(term: String): Boolean {
        return when (this) {
            SystemEntity.Bubbles -> term in "bubbles"
            is SystemEntity.Character -> {
                term in name.lowercase() ||
                    details.corporationName?.let { term in it.lowercase() } ?: false ||
                    details.corporationTicker?.let { term in it.lowercase() } ?: false ||
                    details.allianceName?.let { term in it.lowercase() } ?: false ||
                    details.allianceTicker?.let { term in it.lowercase() } ?: false
            }
            SystemEntity.CombatProbes -> term in "combat probes"
            SystemEntity.Ess -> term in "ess"
            is SystemEntity.Gate -> term in "gate" || term in system
            SystemEntity.GateCamp -> term in "gate camp"
            is SystemEntity.Killmail -> {
                term in "kill" ||
                    ship?.let { term in it.lowercase() } ?: false ||
                    typeName?.let { term in it.lowercase() } ?: false ||
                    victim.corporationName?.let { term in it.lowercase() } ?: false
                victim.corporationTicker?.let { term in it.lowercase() } ?: false
                victim.allianceName?.let { term in it.lowercase() } ?: false
                victim.allianceTicker?.let { term in it.lowercase() } ?: false
            }
            SystemEntity.NoVisual -> term in "no visual" || term in "nv"
            is SystemEntity.Ship -> term in name.lowercase()
            SystemEntity.Skyhook -> term in "skyhook"
            SystemEntity.Spike -> term in "spike"
            is SystemEntity.UnspecifiedCharacter -> term in "hostiles"
            SystemEntity.Wormhole -> term in "wormhole" || term in "wh"
            is CharacterBound -> false
            is Clearable -> false
        }
    }
}
