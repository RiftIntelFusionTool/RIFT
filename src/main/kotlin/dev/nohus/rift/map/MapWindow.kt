package dev.nohus.rift.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
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
import dev.nohus.rift.compose.GetSystemContextMenuItems
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_map
import dev.nohus.rift.map.MapLayoutRepository.Position
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.MapViewModel.Transform
import dev.nohus.rift.map.MapViewModel.UiState
import dev.nohus.rift.map.dataoverlay.HostileEntitiesMapDataOverlayPainter
import dev.nohus.rift.map.painter.MapPainter
import dev.nohus.rift.map.painter.RegionsMapPainter
import dev.nohus.rift.map.painter.SystemsMapPainter
import dev.nohus.rift.map.systemcolor.ActualSolarSystemColorStrategy
import dev.nohus.rift.map.systemcolor.SecuritySolarSystemColorStrategy
import dev.nohus.rift.map.systemcolor.SolarSystemColorStrategy
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.MapStarColor
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
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
        titleBarStyle = if (state.isUsingCompactMode) TitleBarStyle.Small else TitleBarStyle.Full,
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
        withContentPadding = !state.isUsingCompactMode,
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
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it.mapType },
        transitionSpec = { getMapChangeTransition(initialState, targetState) },
        modifier = Modifier
            .background(RiftTheme.colors.mapBackground)
            .border(1.dp, RiftTheme.colors.borderGrey),
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
            isShowingIcons = state.isUsingCompactMode,
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
            height = if (state.isUsingCompactMode) 24.dp else 32.dp,
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
                    ?.let { Offset(it.x.toFloat(), it.y.toFloat()) }
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

    val hostileEntitiesMapDataOverlayPainter = remember(state.mapState.intel) {
        HostileEntitiesMapDataOverlayPainter { system -> state.mapState.intel[system] ?: emptyList() }
    }
    val solarSystemColorStrategy = remember(state.mapType, state.starColor, hostileEntitiesMapDataOverlayPainter) {
        val starColor = when (state.mapType) {
            ClusterSystemsMap -> state.starColor.getValue(SettingsMapType.NewEden)
            is RegionMap -> state.starColor.getValue(SettingsMapType.Region)
            else -> null
        } ?: MapStarColor.Actual
        when (starColor) {
            MapStarColor.Actual -> ActualSolarSystemColorStrategy()
            MapStarColor.Security -> SecuritySolarSystemColorStrategy()
            MapStarColor.IntelHostiles -> hostileEntitiesMapDataOverlayPainter.asSolarSystemColorStrategy()
        }
    }

    val mapPainter: MapPainter = remember(
        state.cluster,
        state.layout,
        state.jumpBridgeAdditionalSystems,
        solarSystemColorStrategy,
        state.mapType,
        state.isJumpBridgeNetworkShown,
        state.jumpBridgeNetworkOpacity,
        state.mapState.autopilotConnections,
    ) {
        if (state.mapType is ClusterRegionsMap) {
            RegionsMapPainter(state.cluster, state.layout)
        } else {
            SystemsMapPainter(
                cluster = state.cluster,
                layout = state.layout,
                jumpBridgeAdditionalSystems = state.jumpBridgeAdditionalSystems,
                solarSystemColorStrategy = solarSystemColorStrategy,
                mapType = state.mapType,
                isJumpBridgeNetworkShown = state.isJumpBridgeNetworkShown,
                jumpBridgeNetworkOpacity = state.jumpBridgeNetworkOpacity,
                autopilotConnections = state.mapState.autopilotConnections,
            )
        }
    }.apply { initializeComposed() }

    LaunchedEffect(state.mapState.selectedSystem) {
        val selectedPosition = state.layout[state.mapState.selectedSystem] ?: return@LaunchedEffect
        center = Offset(selectedPosition.x.toFloat(), selectedPosition.y.toFloat())
    }

    val transition = rememberInfiniteTransition()
    val animationPercentage by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing)),
    )

    Box(modifier = Modifier.clipToBounds()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    event.changes.forEach { change ->
                        var scrollDelta = change.scrollDelta.y
                        if (state.isScrollZoomInverted) scrollDelta = -scrollDelta
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
        ) {
            val baseScale = when (state.mapType) {
                ClusterRegionsMap -> 0.7f
                ClusterSystemsMap -> 2.0f
                is RegionMap -> 0.6f
            }
            val scale = baseScale / animatedZoom / density
            mapScale = scale
            canvasSize = size

            mapPainter.draw(
                scope = this,
                center = DoubleOffset(animatedCenter.x.toDouble(), animatedCenter.y.toDouble()),
                scale = scale,
                zoom = animatedZoom,
                animationPercentage = animationPercentage,
            )
        }
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

@Composable
private fun SolarSystemsLayer(
    state: UiState,
    solarSystemColorStrategy: SolarSystemColorStrategy,
    animatedCenter: Offset,
    mapScale: Float,
    canvasSize: Size,
    nodeSizes: NodeSizes,
) {
    val systemRadialGradients = mutableMapOf<Color, Brush>()
    ForEachSystem(state, animatedCenter, mapScale, canvasSize) { systemId, isHighlightedOrHovered, dpCoordinates, system ->
        val hasIntelPopup = systemId in state.mapState.intelPopupSystems
        val zIndex = if (hasIntelPopup || isHighlightedOrHovered) 1f else 0f

        SolarSystemNode(
            system = system,
            mapType = state.mapType,
            mapScale = mapScale,
            intel = state.mapState.intel[system.id],
            onlineCharacters = state.mapState.onlineCharacterLocations[system.id] ?: emptyList(),
            solarSystemColorStrategy = solarSystemColorStrategy,
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
    ForEachSystem(state, animatedCenter, mapScale, canvasSize) { systemId, isHighlightedOrHovered, dpCoordinates, system ->
        val hasIntelPopup = systemId in state.mapState.intelPopupSystems
        val zIndex = if (hasIntelPopup || isHighlightedOrHovered) 1f else 0f
        val regionName = if (state.mapType is RegionMap && state.mapType.regionId != system.regionId) {
            state.cluster.regions.first { it.id == system.regionId }.name
        } else {
            null
        }

        if ((state.mapType is RegionMap && mapScale <= 0.9f) || (state.mapType is ClusterSystemsMap) || isHighlightedOrHovered) {
            SystemInfoBox(
                system = system,
                regionName = regionName,
                isHighlightedOrHovered = isHighlightedOrHovered,
                intel = state.mapState.intel[system.id],
                hasIntelPopup = hasIntelPopup,
                onlineCharacters = state.mapState.onlineCharacterLocations[system.id] ?: emptyList(),
                onRegionClick = { onRegionClick(system.regionId, system.id) },
                modifier = Modifier
                    .offset(dpCoordinates.first, dpCoordinates.second)
                    .zIndex(zIndex)
                    .offset(x = nodeSizes.radiusWithMargin, y = nodeSizes.radius),
            )
        }
    }
}

@Composable
private fun ForEachSystem(
    state: UiState,
    animatedCenter: Offset,
    mapScale: Float,
    canvasSize: Size,
    content: @Composable (
        systemId: Int,
        isHighlightedOrHovered: Boolean,
        dpCoordinates: Pair<Dp, Dp>,
        system: SolarSystemsRepository.MapSolarSystem,
    ) -> Unit,
) {
    val layout = if (state.isJumpBridgeNetworkShown) {
        state.layout
    } else {
        state.layout - state.jumpBridgeAdditionalSystems
    }
    layout.forEach { (systemId, layoutPosition) ->
        val isHighlightedOrHovered = systemId == state.mapState.hoveredSystem ||
            systemId == state.mapState.selectedSystem ||
            systemId in state.mapState.searchResults
        if (state.mapType is RegionMap || mapScale <= 0.5 || isHighlightedOrHovered) {
            val coordinates = getCanvasCoordinates(layoutPosition.x, layoutPosition.y, animatedCenter, mapScale, canvasSize)
            if (!isOnCanvas(coordinates, canvasSize, 100)) return@forEach
            val dpCoordinates = with(LocalDensity.current) { coordinates.x.toDp() to coordinates.y.toDp() }
            val system = state.cluster.systems.firstOrNull { it.id == systemId } ?: return@forEach
            key(systemId) {
                content(systemId, isHighlightedOrHovered, dpCoordinates, system)
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
    state.layout.forEach { (regionId, layoutPosition) ->
        val isHighlightedOrHovered = regionId == state.mapState.hoveredSystem || regionId == state.mapState.selectedSystem
        val coordinates = getCanvasCoordinates(layoutPosition.x, layoutPosition.y, animatedCenter, mapScale, canvasSize)
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
        val position = state.layout[systemId] ?: return
        val coordinates = getCanvasCoordinates(position.x, position.y, animatedCenter, mapScale, canvasSize)
        val system = state.cluster.systems.first { it.id == systemId }
        key(systemId) {
            RiftContextMenuPopup(
                items = GetSystemContextMenuItems(system.name, systemId, mapType = state.mapType),
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

private fun getMapLayoutBounds(layout: Map<Int, Position>): MapLayoutBounds {
    return MapLayoutBounds(
        minX = layout.minOf { it.value.x },
        maxX = layout.maxOf { it.value.x },
        minY = layout.minOf { it.value.y },
        maxY = layout.maxOf { it.value.y },
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
