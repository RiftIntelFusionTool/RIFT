package dev.nohus.rift.map

import androidx.compose.ui.geometry.Offset
import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.region
import dev.nohus.rift.generated.resources.sun
import dev.nohus.rift.get
import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase.OnlineCharacterLocation
import dev.nohus.rift.map.MapExternalControl.MapExternalControlEvent
import dev.nohus.rift.map.MapLayoutRepository.Position
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.repositories.MapGateConnectionsRepository
import dev.nohus.rift.repositories.MapGateConnectionsRepository.GateConnection
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapConstellation
import dev.nohus.rift.repositories.SolarSystemsRepository.MapRegion
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.settings.persistence.MapStarColor
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.math.pow
import kotlin.math.sqrt
import dev.nohus.rift.settings.persistence.MapType as SettingsMapType

@Single
class MapViewModel(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val gateConnectionsRepository: MapGateConnectionsRepository,
    private val layoutRepository: MapLayoutRepository,
    private val getOnlineCharactersLocationUseCase: GetOnlineCharactersLocationUseCase,
    private val intelStateController: IntelStateController,
    private val mapExternalControl: MapExternalControl,
    private val settings: Settings,
) : ViewModel() {

    data class Cluster(
        val systems: List<MapSolarSystem>,
        val constellations: List<MapConstellation>,
        val regions: List<MapRegion>,
        val connections: List<GateConnection>,
    )

    data class MapState(
        val hoveredSystem: Int? = null,
        val selectedSystem: Int? = null,
        val searchResults: List<Int> = emptyList(),
        val intel: Map<Int, List<IntelStateController.Dated<SystemEntity>>> = emptyMap(),
        val intelPopupSystems: List<Int> = emptyList(),
        val onlineCharacterLocations: Map<Int, List<OnlineCharacterLocation>> = emptyMap(),
        val contextMenuSystem: Int? = null,
        val initialTransform: Transform? = null,
    )

    sealed interface MapType {
        data object ClusterSystemsMap : MapType
        data object ClusterRegionsMap : MapType
        data class RegionMap(val regionId: Int) : MapType
    }

    data class UiState(
        val tabs: List<Tab>,
        val selectedTab: Int,
        val search: String?,
        val starColor: Map<SettingsMapType, MapStarColor>,
        val cluster: Cluster,
        val mapType: MapType,
        val layout: Map<Int, Position>,
        val mapState: MapState = MapState(),
        val isScrollZoomInverted: Boolean,
        val isUsingCompactMode: Boolean,
    )

    private val openRegions = mutableSetOf<Int>()

    data class Transform(val center: Offset, val zoom: Double)
    private val mapTransforms = mutableMapOf<MapType, Transform>()

    private val _state = MutableStateFlow(
        UiState(
            tabs = createTabs(),
            selectedTab = 0,
            search = null,
            starColor = settings.intelMap.mapTypeStarColor,
            cluster = Cluster(
                systems = solarSystemsRepository.mapSolarSystems,
                constellations = solarSystemsRepository.mapConstellations,
                regions = solarSystemsRepository.mapRegions,
                connections = gateConnectionsRepository.gateConnections,
            ),
            mapType = ClusterSystemsMap,
            layout = emptyMap(),
            isScrollZoomInverted = settings.intelMap.isInvertZoom,
            isUsingCompactMode = settings.intelMap.isUsingCompactMode,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            intelStateController.state.collect { updateIntel() }
        }
        viewModelScope.launch {
            getOnlineCharactersLocationUseCase().collect(::onOnlineCharacterLocationsUpdated)
        }
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        starColor = settings.intelMap.mapTypeStarColor,
                        isScrollZoomInverted = settings.intelMap.isInvertZoom,
                        isUsingCompactMode = settings.intelMap.isUsingCompactMode,
                    )
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                updateIntel()
            }
        }
        viewModelScope.launch {
            mapExternalControl.event.collect {
                delay(50) // If this event comes from a context menu, let the menu disappear
                when (val event = it.get()) {
                    is MapExternalControlEvent.ShowSystem -> {
                        openTab(0, event.solarSystemId)
                    }
                    is MapExternalControlEvent.ShowSystemOnRegionMap -> {
                        val regionId = solarSystemsRepository.getRegionIdBySystemId(event.solarSystemId) ?: return@collect
                        openRegionMap(regionId, event.solarSystemId)
                    }
                    null -> {}
                }
            }
        }

        openTab(_state.value.selectedTab, focusedId = null)
    }

    fun onMapHover(offset: Offset, mapScale: Float) {
        if (_state.value.mapType == ClusterRegionsMap) return
        val (closestSystemId, closestSystemLayoutPosition) = _state.value.layout.minBy { (_, position) ->
            (offset.x - position.x).pow(2) + (offset.y - position.y).pow(2)
        }
        val closestSystem = solarSystemsRepository.mapSolarSystems.first { it.id == closestSystemId }
        val distanceInPixels = sqrt((offset.x - closestSystemLayoutPosition.x).pow(2) + (offset.y - closestSystemLayoutPosition.y).pow(2)) / mapScale
        val hoveredSystem = if (distanceInPixels < 10) closestSystem.id else null
        updateMapState { copy(hoveredSystem = hoveredSystem) }
    }

    fun onRegionPointerEnter(regionId: Int) {
        updateMapState { copy(hoveredSystem = regionId) }
    }

    fun onRegionPointerExit(regionId: Int) {
        updateMapState { copy(hoveredSystem = if (hoveredSystem == regionId) null else hoveredSystem) }
    }

    fun onRegionClick(regionId: Int, systemId: Int) {
        openRegionMap(regionId, focusedId = systemId)
    }

    fun onTabClose(tabId: Int) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        val regionId = (tab.payload as? RegionMap)?.regionId ?: return
        closeRegionMap(regionId)
    }

    fun onMapClick(button: Int) {
        val hoveredSystem = _state.value.mapState.hoveredSystem
        if (button == 1) { // Left click
            updateMapState { copy(selectedSystem = hoveredSystem, contextMenuSystem = null) }
            if (state.value.mapType == ClusterRegionsMap && hoveredSystem != null) {
                openRegionMap(hoveredSystem, focusedId = null)
            }
        } else if (button == 3) { // Right click
            updateMapState { copy(contextMenuSystem = hoveredSystem) }
        }
    }

    private fun closeRegionMap(regionId: Int) {
        openRegions -= regionId
        val tabs = createTabs()
        val tabIndex = _state.value.selectedTab.coerceAtMost(tabs.last().id)
        _state.update { it.copy(tabs = tabs) }
        openTab(tabIndex, focusedId = null)
    }

    private fun openRegionMap(regionId: Int, focusedId: Int?) {
        openRegions += regionId
        val tabs = createTabs()
        val tabIndex = tabs.reversed().firstOrNull { (it.payload as? RegionMap)?.regionId == regionId }?.id ?: return
        _state.update { it.copy(tabs = tabs) }
        openTab(tabIndex, focusedId)
    }

    fun onContextMenuDismiss() {
        updateMapState { copy(contextMenuSystem = null) }
    }

    fun onTabSelect(id: Int) {
        openTab(id, focusedId = null)
    }

    fun onMapTransformChanged(mapType: MapType, transform: Transform) {
        mapTransforms[mapType] = transform
    }

    fun onSearchChange(text: String) {
        val search = text.takeIf { it.isNotBlank() }?.trim()
        if (search != null && search.length >= 2) {
            val systemIds = _state.value.cluster.systems
                .filter { text.lowercase() in it.name.lowercase() }
                .map { it.id }
            val regionsIds = _state.value.cluster.regions
                .filter { text.lowercase() in it.name.lowercase() }
                .map { it.id }
            val resultIds = systemIds + regionsIds
            updateMapState { copy(searchResults = resultIds) }
        } else {
            updateMapState { copy(searchResults = emptyList()) }
        }
        _state.update { it.copy(search = search) }
    }

    fun onSearchSubmit() {
        val resultIds = _state.value.mapState.searchResults
        val visibleIds = _state.value.layout.keys
        val visibleResultIds = resultIds.intersect(visibleIds).toList()
        if (visibleResultIds.isEmpty()) return

        val selected = _state.value.mapState.selectedSystem
        var index = visibleResultIds.indexOf(selected) + 1
        if (index > visibleResultIds.lastIndex) index = 0

        updateMapState { copy(selectedSystem = visibleResultIds[index]) }
    }

    private fun openTab(id: Int, focusedId: Int?) {
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        val mapType = tab.payload as? MapType ?: return
        val layout = when (mapType) {
            ClusterSystemsMap -> layoutRepository.getNewEdenLayout()
            ClusterRegionsMap -> layoutRepository.getRegionLayout()
            is RegionMap -> layoutRepository.getLayout(mapType.regionId)
        }

        var selectedId = focusedId ?: getOnlineCharacterLocationId(mapType)
        if (selectedId !in layout.keys) selectedId = null
        val initialTransform = mapTransforms[mapType]

        updateMapState { copy(hoveredSystem = null, selectedSystem = selectedId, contextMenuSystem = null, initialTransform = initialTransform) }
        _state.update { it.copy(selectedTab = id, mapType = mapType, layout = layout) }
    }

    private fun getOnlineCharacterLocationId(mapType: MapType): Int? {
        return _state.value.mapState.onlineCharacterLocations.values
            .flatten()
            .filter {
                when (mapType) {
                    ClusterRegionsMap, ClusterSystemsMap -> true
                    is RegionMap -> it.location.regionId == mapType.regionId
                }
            }
            .map {
                when (mapType) {
                    ClusterRegionsMap -> it.location.regionId
                    ClusterSystemsMap, is RegionMap -> it.location.solarSystemId
                }
            }
            .firstOrNull()
    }

    private fun onOnlineCharacterLocationsUpdated(onlineCharacterLocations: List<OnlineCharacterLocation>) {
        if (settings.intelMap.isCharacterFollowing) {
            val current = _state.value.mapState.onlineCharacterLocations.values.flatten()
            onlineCharacterLocations.forEach { onlineCharacterLocation ->
                val previous = current.firstOrNull { it.id == onlineCharacterLocation.id }
                if (previous?.location?.solarSystemId == onlineCharacterLocation.location.solarSystemId) return@forEach

                val systemId = onlineCharacterLocation.location.solarSystemId
                val regionId = onlineCharacterLocation.location.regionId ?: return@forEach

                when (val mapType = _state.value.mapType) {
                    ClusterRegionsMap -> {
                        updateMapState { copy(selectedSystem = regionId) }
                    }
                    ClusterSystemsMap -> {
                        updateMapState { copy(selectedSystem = systemId) }
                    }
                    is RegionMap -> {
                        if (mapType.regionId == regionId) {
                            updateMapState { copy(selectedSystem = systemId) }
                        } else {
                            openRegionMap(regionId, systemId)
                        }
                    }
                }
            }
        }

        val locations = onlineCharacterLocations.groupBy { it.location.solarSystemId }
        _state.update { it.copy(mapState = it.mapState.copy(onlineCharacterLocations = locations)) }
    }

    private fun createTabs(): List<Tab> {
        return listOf(
            Tab(id = 0, title = "New Eden", isCloseable = false, icon = Res.drawable.sun, payload = ClusterSystemsMap),
            Tab(id = 1, title = "Regions", isCloseable = false, icon = Res.drawable.region, payload = ClusterRegionsMap),
        ) + openRegions.mapIndexed { index, regionId ->
            val name = solarSystemsRepository.mapRegions.firstOrNull { it.id == regionId }?.name ?: "$regionId"
            Tab(id = 2 + index, title = name, isCloseable = true, payload = RegionMap(regionId))
        }
    }

    private fun updateMapState(update: MapState.() -> MapState) {
        _state.update { state -> state.copy(mapState = state.mapState.update()) }
    }

    private fun updateIntel() = viewModelScope.launch {
        val intelBySystemName = intelStateController.state.value
        val intelBySystemId = intelBySystemName.mapKeys { (key, _) ->
            solarSystemsRepository.getSystemId(key)!!
        }

        val now = Instant.now()
        val expiryMinTimestamp = now - Duration.ofSeconds(settings.intelMap.intelExpireSeconds.toLong())
        val popupMinTimestamp = now - Duration.ofSeconds(settings.intelMap.intelPopupTimeoutSeconds.toLong())
        val filtered = intelBySystemId
            .mapValues { (_, datedEntities) ->
                datedEntities.filter { it.timestamp >= expiryMinTimestamp } // Filter out expired intel
            }
            .filter { (_, datedEntities) ->
                datedEntities.isNotEmpty() // Remove systems that no longer have any intel to show
            }
        val popupSystems = filtered.mapNotNull { (systemId, datedEntities) ->
            val showPopup =
                datedEntities.any { it.timestamp >= popupMinTimestamp } || // Only show system if within popup timeout setting
                    systemId == _state.value.mapState.hoveredSystem // Or is hovered
            systemId.takeIf { showPopup }
        }

        updateMapState { copy(intel = filtered, intelPopupSystems = popupSystems) }
    }
}
