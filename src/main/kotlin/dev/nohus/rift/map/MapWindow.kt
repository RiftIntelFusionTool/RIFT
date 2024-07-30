package dev.nohus.rift.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.nohus.rift.compose.GetSystemContextMenuItems
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_map
import dev.nohus.rift.map.MapViewModel.Layout
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.MapViewModel.Transform
import dev.nohus.rift.map.MapViewModel.UiState
import dev.nohus.rift.map.painter.MapPainter
import dev.nohus.rift.map.painter.RegionsMapPainter
import dev.nohus.rift.map.painter.SystemsMapPainter
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.AssetsSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.FactionWarfareSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.HostileEntitiesSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.IncursionsSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.JumpRangeSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.JumpsSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.KillsSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.MetaliminalStormsSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.NpcKillsSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.SecuritySystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.SovereigntySystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.StarColorSystemColorStrategy
import dev.nohus.rift.map.systemcolor.strategies.StationsSystemColorStrategy
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.koin.core.parameter.parametersOf
import kotlin.math.pow
import kotlin.math.roundToInt
import dev.nohus.rift.settings.persistence.MapType as SettingsMapType

@Composable
fun MapWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
    onTuneClick: () -> Unit,
) {
    val viewModel: MapViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Intel Map",
        icon = Res.drawable.window_map,
        state = windowState,
        onTuneClick = onTuneClick,
        onCloseClick = onCloseRequest,
        titleBarStyle = if (state.settings.isUsingCompactMode) TitleBarStyle.Small else TitleBarStyle.Full,
        titleBarContent = { height ->
            ToolbarRow(
                state = state,
                fixedHeight = height,
                onTabSelect = viewModel::onTabSelect,
                onTabClose = viewModel::onTabClose,
                onSearchChange = viewModel::onSearchChange,
                onSearchSubmit = viewModel::onSearchSubmit,
            )
        },
        withContentPadding = !state.settings.isUsingCompactMode,
    ) {
        MapWindowContent(
            state = state,
            onMapHover = viewModel::onMapHover,
            onRegionPointerEnter = viewModel::onRegionPointerEnter,
            onRegionPointerExit = viewModel::onRegionPointerExit,
            onRegionClick = viewModel::onRegionClick,
            onMapClick = viewModel::onMapClick,
            onContextMenuDismiss = viewModel::onContextMenuDismiss,
            onMapTransformChanged = viewModel::onMapTransformChanged,
            onSystemColorChange = viewModel::onSystemColorChange,
            onSystemColorHover = viewModel::onSystemColorHover,
            onCellColorChange = viewModel::onCellColorChange,
            onCellColorHover = viewModel::onCellColorHover,
            onIndicatorChange = viewModel::onIndicatorChange,
            onInfoBoxChange = viewModel::onInfoBoxChange,
            onJumpRangeTargetUpdate = viewModel::onJumpRangeTargetUpdate,
            onJumpRangeDistanceUpdate = viewModel::onJumpRangeDistanceUpdate,
            onPlanetTypesUpdate = viewModel::onPlanetTypesUpdate,
        )
    }
}

data class DoubleOffset(val x: Double, val y: Double) {
    operator fun plus(other: DoubleOffset) = DoubleOffset(x + other.x, y + other.y)
}

@Composable
private fun MapWindowContent(
    state: UiState,
    onMapHover: (Offset, mapScale: Float) -> Unit,
    onRegionPointerEnter: (Int) -> Unit,
    onRegionPointerExit: (Int) -> Unit,
    onRegionClick: (regionId: Int, systemId: Int) -> Unit,
    onMapClick: (button: Int) -> Unit,
    onContextMenuDismiss: () -> Unit,
    onMapTransformChanged: (MapType, Transform) -> Unit,
    onSystemColorChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onSystemColorHover: (SettingsMapType, MapSystemInfoType, Boolean) -> Unit,
    onCellColorChange: (SettingsMapType, MapSystemInfoType?) -> Unit,
    onCellColorHover: (SettingsMapType, MapSystemInfoType?, Boolean) -> Unit,
    onIndicatorChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onInfoBoxChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onJumpRangeTargetUpdate: (String) -> Unit,
    onJumpRangeDistanceUpdate: (Double) -> Unit,
    onPlanetTypesUpdate: (List<PlanetType>) -> Unit,
) {
    Box {
        val hazeState = remember { HazeState() }
        AnimatedContent(
            targetState = state,
            contentKey = { it.mapType },
            transitionSpec = { getMapChangeTransition(initialState, targetState) },
            modifier = Modifier
                .background(RiftTheme.colors.mapBackground)
                .border(1.dp, RiftTheme.colors.borderGrey)
                .haze(hazeState),
        ) { state ->
            Map(
                state = state,
                onMapHover = onMapHover,
                onRegionPointerEnter = onRegionPointerEnter,
                onRegionPointerExit = onRegionPointerExit,
                onRegionClick = onRegionClick,
                onMapClick = onMapClick,
                onContextMenuDismiss = onContextMenuDismiss,
                onMapTransformChanged = { onMapTransformChanged(state.mapType, it) },
            )
        }
        MapSettingsPanel(
            hazeState = hazeState,
            mapType = state.mapType,
            systemInfoTypes = state.systemInfoTypes,
            mapJumpRangeState = state.mapJumpRangeState,
            mapPlanetsState = state.mapPlanetsState,
            onSystemColorChange = onSystemColorChange,
            onSystemColorHover = onSystemColorHover,
            onCellColorChange = onCellColorChange,
            onCellColorHover = onCellColorHover,
            onIndicatorChange = onIndicatorChange,
            onInfoBoxChange = onInfoBoxChange,
            onJumpRangeTargetUpdate = onJumpRangeTargetUpdate,
            onJumpRangeDistanceUpdate = onJumpRangeDistanceUpdate,
            onPlanetTypesUpdate = onPlanetTypesUpdate,
        )
    }
}

@Composable
private fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onTabSelect: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiftTabBar(
            tabs = state.tabs,
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelect,
            onTabClosed = onTabClose,
            withUnderline = false,
            isShowingIcons = state.settings.isUsingCompactMode,
            fixedHeight = fixedHeight,
            modifier = Modifier.weight(1f),
        )
        var search by remember { mutableStateOf(state.search ?: "") }
        val focusManager = LocalFocusManager.current
        RiftTextField(
            text = search,
            placeholder = "Search",
            onTextChanged = {
                search = it
                onSearchChange(it)
            },
            height = if (state.settings.isUsingCompactMode) 24.dp else 32.dp,
            onDeleteClick = {
                search = ""
                onSearchChange("")
            },
            modifier = Modifier
                .width(100.dp)
                .padding(start = Spacing.medium)
                .onKeyEvent {
                    when (it.key) {
                        Key.Enter -> {
                            onSearchSubmit()
                            true
                        }
                        Key.Escape -> {
                            focusManager.clearFocus()
                            true
                        }
                        else -> false
                    }
                },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Map(
    state: UiState,
    onMapHover: (Offset, mapScale: Float) -> Unit,
    onRegionPointerEnter: (Int) -> Unit,
    onRegionPointerExit: (Int) -> Unit,
    onRegionClick: (regionId: Int, systemId: Int) -> Unit,
    onMapClick: (button: Int) -> Unit,
    onContextMenuDismiss: () -> Unit,
    onMapTransformChanged: (Transform) -> Unit,
) {
    val layoutBounds by remember { mutableStateOf(getMapLayoutBounds(state.layout)) }
    var zoom by remember {
        mutableStateOf(
            state.mapState.initialTransform?.zoom ?: if (state.mapType is ClusterSystemsMap) 0.2 else 1.0,
        )
    }
    val animatedZoom by animateFloatAsState(zoom.toFloat(), spring(stiffness = Spring.StiffnessLow))
    var center by remember {
        mutableStateOf(
            if (state.mapState.selectedSystem != null) {
                state.layout[state.mapState.selectedSystem]
                    ?.let { Offset(it.position.x.toFloat(), it.position.y.toFloat()) }
                    ?: getMapLayoutCenter(layoutBounds)
            } else {
                state.mapState.initialTransform?.center ?: getMapLayoutCenter(layoutBounds)
            },
        )
    }
    LaunchedEffect(center, zoom) {
        onMapTransformChanged(Transform(center, zoom))
    }

    val springSpec = remember { spring<Offset>(stiffness = Spring.StiffnessLow) }
    val snapSpec = remember { snap<Offset>() }
    var translationAnimationSpec by remember { mutableStateOf<AnimationSpec<Offset>>(springSpec) }
    val animatedCenter by animateOffsetAsState(Offset(center.x, center.y), translationAnimationSpec)
    if (translationAnimationSpec != springSpec && animatedCenter == center) {
        translationAnimationSpec = springSpec
    }
    val dragMatcher =
        remember { PointerMatcher.mouse(PointerButton.Primary) + PointerMatcher.mouse(PointerButton.Secondary) }

    var mapScale by remember { mutableStateOf(0.0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val hostileEntitiesSystemColorStrategy = remember(state.mapState.intel) {
        HostileEntitiesSystemColorStrategy { system -> state.mapState.intel[system].orEmpty() }
    }
    val systemStatusColorStrategies = remember(state.mapState.systemStatus) {
        SystemStatusColorStrategies(
            jumps = JumpsSystemColorStrategy(state.mapState.systemStatus),
            kills = KillsSystemColorStrategy(state.mapState.systemStatus),
            npcKills = NpcKillsSystemColorStrategy(state.mapState.systemStatus),
            assets = AssetsSystemColorStrategy(state.mapState.systemStatus),
            incursions = IncursionsSystemColorStrategy(state.mapState.systemStatus),
            stations = StationsSystemColorStrategy(state.mapState.systemStatus),
            factionWarfare = FactionWarfareSystemColorStrategy(state.mapState.systemStatus),
            sovereignty = koin.get { parametersOf(state.mapState.systemStatus) },
            storms = MetaliminalStormsSystemColorStrategy(state.mapState.systemStatus),
            jumpRange = JumpRangeSystemColorStrategy(state.mapState.systemStatus),
        )
    }

    val mapPainter: MapPainter = remember(
        state.cluster,
        state.layout,
        state.jumpBridgeAdditionalSystems,
        state.mapType,
        state.settings.isJumpBridgeNetworkShown,
        state.settings.jumpBridgeNetworkOpacity,
        state.mapState.autopilotConnections,
    ) {
        if (state.mapType is ClusterRegionsMap) {
            RegionsMapPainter(state.cluster, state.layout)
        } else {
            SystemsMapPainter(
                cluster = state.cluster,
                layout = state.layout,
                jumpBridgeAdditionalSystems = state.jumpBridgeAdditionalSystems,
                mapType = state.mapType,
                isJumpBridgeNetworkShown = state.settings.isJumpBridgeNetworkShown,
                jumpBridgeNetworkOpacity = state.settings.jumpBridgeNetworkOpacity,
                autopilotConnections = state.mapState.autopilotConnections,
            )
        }
    }.apply { initializeComposed() }

    LaunchedEffect(state.mapState.selectedSystem) {
        val selectedPosition = state.layout[state.mapState.selectedSystem]?.position ?: return@LaunchedEffect
        center = Offset(selectedPosition.x.toFloat(), selectedPosition.y.toFloat())
    }

    val transition = rememberInfiniteTransition()
    val animationPercentage by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing)),
    )

    Crossfade(targetState = state.systemInfoTypes) { systemInfoTypes ->
        val solarSystemColorStrategy = remember(
            state.mapType,
            systemInfoTypes.starApplied,
            hostileEntitiesSystemColorStrategy,
            systemStatusColorStrategies,
        ) {
            getSolarSystemColorStrategy(
                state.mapType,
                systemInfoTypes.starApplied,
                hostileEntitiesSystemColorStrategy,
                systemStatusColorStrategies,
            )!!
        }
        val cellColorStrategy = remember(
            state.mapType,
            systemInfoTypes.cellApplied,
            hostileEntitiesSystemColorStrategy,
            systemStatusColorStrategies,
        ) {
            getSolarSystemColorStrategy(
                state.mapType,
                systemInfoTypes.cellApplied,
                hostileEntitiesSystemColorStrategy,
                systemStatusColorStrategies,
            )
        }
        Box(modifier = Modifier.clipToBounds()) {
            val baseScale = when (state.mapType) {
                ClusterRegionsMap -> 0.7f
                ClusterSystemsMap -> 2.0f
                is RegionMap -> 0.6f
            }
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                val scale = baseScale / animatedZoom / density
                mapScale = scale
                canvasSize = size
                mapPainter.drawStatic(
                    scope = this,
                    center = DoubleOffset(animatedCenter.x.toDouble(), animatedCenter.y.toDouble()),
                    scale = scale,
                    zoom = animatedZoom,
                    systemColorStrategy = solarSystemColorStrategy,
                    cellColorStrategy = cellColorStrategy,
                )
            }
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                val scale = baseScale / animatedZoom / density
                mapPainter.drawAnimated(
                    scope = this,
                    center = DoubleOffset(animatedCenter.x.toDouble(), animatedCenter.y.toDouble()),
                    scale = scale,
                    zoom = animatedZoom,
                    animationPercentage = animationPercentage,
                    systemColorStrategy = solarSystemColorStrategy,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        event.changes.forEach { change ->
                            var scrollDelta = change.scrollDelta.y
                            if (state.settings.isInvertZoom) scrollDelta = -scrollDelta
                            val zoomPercentDelta = 1.3.pow(scrollDelta.toDouble())
                            val zoomRange = when (state.mapType) {
                                ClusterRegionsMap -> 1.0..2.0
                                ClusterSystemsMap -> 0.2..8.0
                                is RegionMap -> 0.3..2.0
                            }
                            zoom = (zoom * zoomPercentDelta).coerceIn(zoomRange)
                        }
                    }
                    .onDrag(matcher = dragMatcher) {
                        if (translationAnimationSpec != snapSpec) {
                            translationAnimationSpec = snapSpec
                        }
                        center = Offset(
                            x = (center.x + (-it.x * mapScale)).coerceIn(layoutBounds.minX.toFloat()..layoutBounds.maxX.toFloat()),
                            y = (center.y + (-it.y * mapScale)).coerceIn(layoutBounds.minY.toFloat()..layoutBounds.maxY.toFloat()),
                        )
                    }
                    .onPointerEvent(PointerEventType.Move) { event ->
                        event.changes.forEach { change ->
                            val centerOffset = Offset(
                                change.position.x - canvasSize.width / 2,
                                change.position.y - canvasSize.height / 2,
                            )
                            val layoutOffset = center.copy(
                                x = center.x + centerOffset.x * mapScale,
                                y = center.y + centerOffset.y * mapScale,
                            )
                            onMapHover(layoutOffset, mapScale)
                        }
                    }
                    .onPointerEvent(PointerEventType.Release) { event ->
                        val awtEvent = event.awtEventOrNull ?: return@onPointerEvent
                        onMapClick(awtEvent.button)
                    },
            )
            if (mapScale != 0.0f && canvasSize != Size.Zero) {
                when (state.mapType) {
                    ClusterRegionsMap -> {
                        RegionsLayer(state, animatedCenter, mapScale, canvasSize, onRegionPointerEnter, onRegionPointerExit, onClick = { onMapClick(1) })
                    }
                    ClusterSystemsMap, is RegionMap -> {
                        val nodeSizes = NodeSizes(
                            margin = 16.dp,
                            marginPx = LocalDensity.current.run { 12.dp.toPx() },
                            radius = 8.dp,
                            radiusPx = LocalDensity.current.run { 8.dp.toPx() },
                        )
                        SolarSystemsLayer(state, solarSystemColorStrategy, animatedCenter, mapScale, canvasSize, nodeSizes)
                        SystemInfoBoxesLayer(state, animatedCenter, mapScale, canvasSize, nodeSizes, onRegionClick)
                        SystemContextMenu(state, animatedCenter, mapScale, canvasSize, onContextMenuDismiss)
                    }
                }
            }
        }
    }
}

data class SystemStatusColorStrategies(
    val jumps: JumpsSystemColorStrategy,
    val kills: KillsSystemColorStrategy,
    val npcKills: NpcKillsSystemColorStrategy,
    val assets: AssetsSystemColorStrategy,
    val incursions: IncursionsSystemColorStrategy,
    val stations: StationsSystemColorStrategy,
    val factionWarfare: FactionWarfareSystemColorStrategy,
    val sovereignty: SovereigntySystemColorStrategy,
    val storms: MetaliminalStormsSystemColorStrategy,
    val jumpRange: JumpRangeSystemColorStrategy,
)

fun getSolarSystemColorStrategy(
    mapType: MapType,
    color: Map<SettingsMapType, MapSystemInfoType?>,
    hostileEntitiesSystemColorStrategy: HostileEntitiesSystemColorStrategy,
    systemStatusColorStrategies: SystemStatusColorStrategies,
): SystemColorStrategy? {
    val color = when (mapType) {
        is ClusterSystemsMap -> color[SettingsMapType.NewEden]
        is ClusterRegionsMap -> color[SettingsMapType.NewEden]
        is RegionMap -> color[SettingsMapType.Region]
    } ?: return null
    return when (color) {
        MapSystemInfoType.StarColor -> koin.get<StarColorSystemColorStrategy>()
        MapSystemInfoType.Security -> koin.get<SecuritySystemColorStrategy>()
        MapSystemInfoType.IntelHostiles -> hostileEntitiesSystemColorStrategy
        MapSystemInfoType.Jumps -> systemStatusColorStrategies.jumps
        MapSystemInfoType.Kills -> systemStatusColorStrategies.kills
        MapSystemInfoType.NpcKills -> systemStatusColorStrategies.npcKills
        MapSystemInfoType.Assets -> systemStatusColorStrategies.assets
        MapSystemInfoType.Incursions -> systemStatusColorStrategies.incursions
        MapSystemInfoType.Stations -> systemStatusColorStrategies.stations
        MapSystemInfoType.FactionWarfare -> systemStatusColorStrategies.factionWarfare
        MapSystemInfoType.Sovereignty -> systemStatusColorStrategies.sovereignty
        MapSystemInfoType.MetaliminalStorms -> systemStatusColorStrategies.storms
        MapSystemInfoType.JumpRange -> systemStatusColorStrategies.jumpRange
        MapSystemInfoType.Planets -> throw IllegalArgumentException("Not used for coloring")
    }
}

@Composable
private fun SolarSystemsLayer(
    state: UiState,
    systemColorStrategy: SystemColorStrategy,
    animatedCenter: Offset,
    mapScale: Float,
    canvasSize: Size,
    nodeSizes: NodeSizes,
) {
    val systemRadialGradients = mutableMapOf<Color, Brush>()
    ForEachSystem(state, animatedCenter, mapScale, canvasSize) { isHighlightedOrHovered, dpCoordinates, system ->
        val hasIntelPopup = system.id in state.mapState.intelPopupSystems
        val zIndex = if (hasIntelPopup || isHighlightedOrHovered) 1f else 0f

        SolarSystemNode(
            system = system,
            mapType = state.mapType,
            mapScale = mapScale,
            intel = state.mapState.intel[system.id],
            onlineCharacters = state.mapState.onlineCharacterLocations[system.id].orEmpty(),
            systemColorStrategy = systemColorStrategy,
            systemRadialGradients = systemRadialGradients,
            nodeSizes = nodeSizes,
            modifier = Modifier
                .offset(dpCoordinates.first, dpCoordinates.second)
                .zIndex(zIndex),
        )
    }
}

@Composable
private fun SystemInfoBoxesLayer(
    state: UiState,
    animatedCenter: Offset,
    mapScale: Float,
    canvasSize: Size,
    nodeSizes: NodeSizes,
    onRegionClick: (regionId: Int, systemId: Int) -> Unit,
) {
    val indicatorsInfoTypes = getIndicatorsInfoTypes(state)
    val infoBoxInfoTypes = getInfoBoxInfoTypes(state)
    ForEachSystem(state, animatedCenter, mapScale, canvasSize) { isHighlightedOrHovered, dpCoordinates, system ->
        val hasIntelPopup = system.id in state.mapState.intelPopupSystems
        val zIndex = if (hasIntelPopup || isHighlightedOrHovered) 1f else 0f
        val regionName = if (state.mapType is RegionMap && state.mapType.regionId != system.regionId) {
            state.cluster.regions.first { it.id == system.regionId }.name
        } else {
            null
        }

        if ((state.mapType is RegionMap && mapScale <= (0.9f / LocalDensity.current.density)) || (state.mapType is ClusterSystemsMap) || isHighlightedOrHovered) {
            SystemInfoBox(
                system = system,
                regionName = regionName,
                isHighlightedOrHovered = isHighlightedOrHovered,
                intel = state.mapState.intel[system.id],
                hasIntelPopup = hasIntelPopup,
                onlineCharacters = state.mapState.onlineCharacterLocations[system.id].orEmpty(),
                systemStatus = state.mapState.systemStatus[system.id],
                infoTypes = infoBoxInfoTypes,
                indicatorsInfoTypes = indicatorsInfoTypes,
                onRegionClick = { onRegionClick(system.regionId, system.id) },
                modifier = Modifier
                    .offset(dpCoordinates.first, dpCoordinates.second)
                    .zIndex(zIndex)
                    .offset(x = nodeSizes.radiusWithMargin, y = nodeSizes.radius),
            )
        }
    }
}

private fun getIndicatorsInfoTypes(state: UiState): List<MapSystemInfoType> {
    val settingsMapType = getSettingsMapType(state.mapType)
    return state.systemInfoTypes.indicators[settingsMapType].orEmpty()
}

private fun getInfoBoxInfoTypes(state: UiState): List<MapSystemInfoType> {
    val settingsMapType = getSettingsMapType(state.mapType)
    return state.systemInfoTypes.infoBox[settingsMapType].orEmpty()
}

private fun getSettingsMapType(mapType: MapType): SettingsMapType {
    return when (mapType) {
        ClusterRegionsMap -> SettingsMapType.NewEden
        ClusterSystemsMap -> SettingsMapType.NewEden
        is RegionMap -> SettingsMapType.Region
    }
}

@Composable
private fun ForEachSystem(
    state: UiState,
    animatedCenter: Offset,
    mapScale: Float,
    canvasSize: Size,
    content: @Composable (
        isHighlightedOrHovered: Boolean,
        dpCoordinates: Pair<Dp, Dp>,
        system: SolarSystemsRepository.MapSolarSystem,
    ) -> Unit,
) {
    val layout = if (state.settings.isJumpBridgeNetworkShown) {
        state.layout
    } else {
        state.layout - state.jumpBridgeAdditionalSystems
    }
    layout.forEach { (systemId, layout) ->
        val isHighlightedOrHovered = systemId == state.mapState.hoveredSystem ||
            systemId == state.mapState.selectedSystem ||
            systemId in state.mapState.searchResults
        if (state.mapType is RegionMap || mapScale <= 0.5 || isHighlightedOrHovered) {
            val coordinates = getCanvasCoordinates(layout.position.x, layout.position.y, animatedCenter, mapScale, canvasSize)
            if (!isOnCanvas(coordinates, canvasSize, 100)) return@forEach
            val dpCoordinates = with(LocalDensity.current) { coordinates.x.toDp() to coordinates.y.toDp() }
            val system = state.cluster.systems.firstOrNull { it.id == systemId } ?: return@forEach
            key(systemId) {
                content(isHighlightedOrHovered, dpCoordinates, system)
            }
        }
    }
}

@Composable
private fun RegionsLayer(
    state: UiState,
    animatedCenter: Offset,
    mapScale: Float,
    canvasSize: Size,
    onPointerEnter: (Int) -> Unit,
    onPointerExit: (Int) -> Unit,
    onClick: () -> Unit,
) {
    state.layout.forEach { (regionId, layout) ->
        val isHighlightedOrHovered = regionId == state.mapState.hoveredSystem || regionId == state.mapState.selectedSystem
        val coordinates = getCanvasCoordinates(layout.position.x, layout.position.y, animatedCenter, mapScale, canvasSize)
        val dpCoordinates = with(LocalDensity.current) { coordinates.x.toDp() to coordinates.y.toDp() }
        key(regionId) {
            val region = state.cluster.regions.firstOrNull { it.id == regionId } ?: return@forEach
            RegionNode(
                region = region,
                mapScale = mapScale,
                onlineCharacters = state.mapState.onlineCharacterLocations.values.flatten(),
                isHighlightedOrHovered = isHighlightedOrHovered,
                onPointerEnter = { onPointerEnter(region.id) },
                onPointerExit = { onPointerExit(region.id) },
                onClick = onClick,
                modifier = Modifier
                    .offset(dpCoordinates.first, dpCoordinates.second),
            )
        }
    }
}

@Composable
private fun SystemContextMenu(
    state: UiState,
    animatedCenter: Offset,
    mapScale: Float,
    canvasSize: Size,
    onDismissRequest: () -> Unit,
) {
    state.mapState.contextMenuSystem?.let { systemId ->
        val position = state.layout[systemId]?.position ?: return
        val coordinates = getCanvasCoordinates(position.x, position.y, animatedCenter, mapScale, canvasSize)
        key(systemId) {
            RiftContextMenuPopup(
                items = GetSystemContextMenuItems(systemId, mapType = state.mapType),
                offset = IntOffset(coordinates.x.toInt(), coordinates.y.toInt()),
                onDismissRequest = onDismissRequest,
            )
        }
    }
}

private fun getMapChangeTransition(from: UiState, to: UiState): ContentTransform {
    val fromType = from.mapType
    val toType = to.mapType
    val duration = 500
    return when {
        fromType is ClusterRegionsMap && toType is RegionMap || fromType is ClusterSystemsMap && toType is RegionMap -> {
            // Zoom in
            (fadeIn(animationSpec = tween(duration)) + scaleIn(initialScale = 0.7f, animationSpec = tween(duration)))
                .togetherWith(fadeOut(animationSpec = tween(duration / 2)) + scaleOut(targetScale = 1.3f, animationSpec = tween(duration / 2)))
        }
        fromType is RegionMap && toType is ClusterRegionsMap || fromType is RegionMap && toType is ClusterSystemsMap -> {
            // Zoom out
            (fadeIn(animationSpec = tween(duration)) + scaleIn(initialScale = 1.3f, animationSpec = tween(duration)))
                .togetherWith(fadeOut(animationSpec = tween(duration / 2)) + scaleOut(targetScale = 0.7f, animationSpec = tween(duration / 2)))
        }
        else -> {
            // Crossfade
            (fadeIn(animationSpec = tween(duration, delayMillis = (duration * 0.4).roundToInt())) + scaleIn(initialScale = 0.92f, animationSpec = tween(duration, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween((duration * 0.4).roundToInt())))
        }
    }
}

data class MapLayoutBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
)

private fun getMapLayoutBounds(layout: Map<Int, Layout>): MapLayoutBounds {
    return MapLayoutBounds(
        minX = layout.minOf { it.value.position.x },
        maxX = layout.maxOf { it.value.position.x },
        minY = layout.minOf { it.value.position.y },
        maxY = layout.maxOf { it.value.position.y },
    )
}

private fun getMapLayoutCenter(bounds: MapLayoutBounds): Offset {
    val centerX = (bounds.maxX + bounds.minX) / 2
    val centerY = (bounds.maxY + bounds.minY) / 2
    return Offset(centerX.toFloat(), centerY.toFloat())
}

private fun getCanvasCoordinates(x: Int, y: Int, center: Offset, scale: Float, canvasSize: Size): Offset {
    val canvasX = (x - center.x) / scale + canvasSize.center.x
    val canvasY = (y - center.y) / scale + canvasSize.center.y
    return Offset(canvasX, canvasY)
}

private fun isOnCanvas(offset: Offset, canvasSize: Size, margin: Int = 0): Boolean {
    return offset.x >= -margin && offset.y >= -margin && offset.x < canvasSize.width + margin && offset.y < canvasSize.height + margin
}
