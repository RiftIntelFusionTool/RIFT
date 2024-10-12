package dev.nohus.rift.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftPill
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.backicon
import dev.nohus.rift.generated.resources.expand_more_16px
import dev.nohus.rift.map.MapJumpRangeController.MapJumpRangeState
import dev.nohus.rift.map.MapPlanetsController.MapPlanetsState
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.MapViewModel.SystemInfoTypes
import dev.nohus.rift.map.PanelState.CellColor
import dev.nohus.rift.map.PanelState.Collapsed
import dev.nohus.rift.map.PanelState.Expanded
import dev.nohus.rift.map.PanelState.Indicators
import dev.nohus.rift.map.PanelState.InfoBox
import dev.nohus.rift.map.PanelState.JumpRange
import dev.nohus.rift.map.PanelState.Planets
import dev.nohus.rift.map.PanelState.StarColor
import dev.nohus.rift.repositories.PlanetTypes
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import org.jetbrains.compose.resources.painterResource
import dev.nohus.rift.settings.persistence.MapType as SettingsMapType

enum class PanelState {
    Collapsed, Expanded,
    StarColor, CellColor, Indicators, InfoBox,
    JumpRange, Planets
}

private val editableInfoTypes = mapOf(
    MapSystemInfoType.JumpRange to JumpRange,
    MapSystemInfoType.Planets to Planets,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapSettingsPanel(
    hazeState: HazeState,
    mapType: MapType,
    systemInfoTypes: SystemInfoTypes,
    mapJumpRangeState: MapJumpRangeState,
    mapPlanetsState: MapPlanetsState,
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
    val settingsMapType = when (mapType) {
        ClusterRegionsMap -> null
        ClusterSystemsMap -> SettingsMapType.NewEden
        is RegionMap -> SettingsMapType.Region
    } ?: return
    Column(
        modifier = Modifier.padding(1.dp),
    ) {
        var previousPanelState: PanelState by remember { mutableStateOf(Collapsed) }
        var panelState: PanelState by remember { mutableStateOf(Collapsed) }
        ScrollbarColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            isScrollbarConditional = true,
            hasScrollbarBackground = false,
            modifier = Modifier
                .heightIn(max = 200.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    if (panelState == Collapsed) panelState = Expanded
                }
                .onPointerEvent(PointerEventType.Exit) {
                    if (panelState == Expanded) panelState = Collapsed
                }
                .hazeChild(hazeState),
        ) {
            AnimatedContent(targetState = panelState) { state ->
                when (state) {
                    Collapsed -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.expand_more_16px),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = Spacing.small)
                                    .size(16.dp),
                            )
                        }
                    }
                    Expanded -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier
                                .padding(Spacing.medium)
                                .fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),

                            ) {
                                Text(
                                    text = "System:",
                                    style = RiftTheme.typography.titlePrimary,
                                )
                                SystemColorPills(
                                    isExpanded = false,
                                    isCellColor = false,
                                    selected = systemInfoTypes.starSelected[settingsMapType],
                                    onPillClick = {
                                        panelState = StarColor
                                    },
                                    onPillEditClick = {
                                        previousPanelState = panelState
                                        panelState = editableInfoTypes[it]!!
                                    },
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                Text(
                                    text = "Background:",
                                    style = RiftTheme.typography.titlePrimary,
                                )
                                SystemColorPills(
                                    isExpanded = false,
                                    isCellColor = true,
                                    selected = systemInfoTypes.cellSelected[settingsMapType],
                                    onPillClick = {
                                        panelState = CellColor
                                    },
                                    onPillEditClick = {
                                        previousPanelState = panelState
                                        panelState = editableInfoTypes[it]!!
                                    },
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                Text(
                                    text = "Indicators:",
                                    style = RiftTheme.typography.titlePrimary,
                                )
                                val text = systemInfoTypes.indicators[settingsMapType].orEmpty().let {
                                    if (it.isEmpty()) "None" else "${it.size} enabled"
                                }
                                RiftPill(
                                    text = text,
                                    onClick = {
                                        panelState = Indicators
                                    },
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                Text(
                                    text = "Info box:",
                                    style = RiftTheme.typography.titlePrimary,
                                )
                                val text = systemInfoTypes.infoBox[settingsMapType].orEmpty().let {
                                    if (it.isEmpty()) "None" else "${it.size} enabled"
                                }
                                RiftPill(
                                    text = text,
                                    onClick = {
                                        panelState = InfoBox
                                    },
                                )
                            }
                        }
                    }
                    StarColor -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "System color",
                                onBack = { panelState = Expanded },
                            )
                            SystemColorPills(
                                isExpanded = true,
                                isCellColor = false,
                                selected = systemInfoTypes.starSelected[settingsMapType],
                                onPillClick = {
                                    onSystemColorChange(settingsMapType, it!!)
                                    panelState = Expanded
                                },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                                onPillHover = { color, isHovered ->
                                    onSystemColorHover(settingsMapType, color!!, isHovered)
                                },
                            )
                        }
                    }
                    CellColor -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "System background color",
                                onBack = { panelState = Expanded },
                            )
                            SystemColorPills(
                                isExpanded = true,
                                isCellColor = true,
                                selected = systemInfoTypes.cellSelected[settingsMapType],
                                onPillClick = {
                                    onCellColorChange(settingsMapType, it)
                                    panelState = Expanded
                                },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                                onPillHover = { color, isHovered ->
                                    onCellColorHover(settingsMapType, color, isHovered)
                                },
                            )
                        }
                    }
                    Indicators -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "Indicators (always visible)",
                                onBack = { panelState = Expanded },
                            )
                            SystemIndicatorsPills(
                                hidden = setOf(
                                    MapSystemInfoType.StarColor,
                                    MapSystemInfoType.NullSecurity,
                                    MapSystemInfoType.IntelHostiles,
                                    MapSystemInfoType.FactionWarfare,
                                ),
                                getInfoTypeNames = ::getMapStarInfoTypeIndicatorName,
                                selected = systemInfoTypes.indicators[settingsMapType].orEmpty(),
                                onPillClick = { onIndicatorChange(settingsMapType, it) },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                            )
                        }
                    }
                    InfoBox -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "Info box details (visible on hover)",
                                onBack = { panelState = Expanded },
                            )
                            SystemIndicatorsPills(
                                hidden = setOf(
                                    MapSystemInfoType.StarColor,
                                    MapSystemInfoType.NullSecurity,
                                    MapSystemInfoType.IntelHostiles,
                                ),
                                getInfoTypeNames = ::getMapStarInfoTypeInfoBoxName,
                                selected = systemInfoTypes.infoBox[settingsMapType].orEmpty(),
                                onPillClick = { onInfoBoxChange(settingsMapType, it) },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                            )
                        }
                    }
                    JumpRange -> {
                        JumpRangePanel(
                            mapJumpRangeState = mapJumpRangeState,
                            onBack = { panelState = previousPanelState },
                            onJumpRangeTargetUpdate = onJumpRangeTargetUpdate,
                            onJumpRangeDistanceUpdate = onJumpRangeDistanceUpdate,
                        )
                    }
                    Planets -> {
                        PlanetsPanel(
                            mapPlanetsState = mapPlanetsState,
                            onBack = { panelState = previousPanelState },
                            onPlanetTypesUpdate = onPlanetTypesUpdate,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(RiftTheme.colors.borderGrey),
        )
    }
}

@Composable
private fun JumpRangePanel(
    mapJumpRangeState: MapJumpRangeState,
    onBack: () -> Unit,
    onJumpRangeTargetUpdate: (String) -> Unit,
    onJumpRangeDistanceUpdate: (Double) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = "Jump Range",
            onBack = onBack,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            var targetText by remember { mutableStateOf("") }
            LaunchedEffect(mapJumpRangeState.target) {
                when (mapJumpRangeState.target) {
                    is MapJumpRangeController.MapJumpRangeTarget.Character -> targetText = mapJumpRangeState.target.name
                    is MapJumpRangeController.MapJumpRangeTarget.System -> targetText = mapJumpRangeState.target.name
                    null -> {}
                }
            }
            Text(
                text = "From:",
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
            RiftTextField(
                text = targetText,
                placeholder = "System or character",
                onTextChanged = {
                    targetText = it
                    onJumpRangeTargetUpdate(it)
                },
                modifier = Modifier
                    .width(150.dp),
            )
            AnimatedVisibility(targetText.isNotBlank()) {
                RequirementIcon(
                    isFulfilled = mapJumpRangeState.target != null,
                    fulfilledTooltip = when (mapJumpRangeState.target) {
                        is MapJumpRangeController.MapJumpRangeTarget.Character -> "Valid character"
                        is MapJumpRangeController.MapJumpRangeTarget.System -> "Valid system"
                        null -> ""
                    },
                    notFulfilledTooltip = "No such system or character",
                )
            }
        }
        val ranges = listOf(
            "6ly – Supercarriers, Titans" to 6.0,
            "7ly – Carriers, Dreadnoughts, Faxes" to 7.0,
            "8ly – Black Ops" to 8.0,
            "10ly – Jump Freighters, Rorquals" to 10.0,
        )
        RiftDropdownWithLabel(
            label = "Range:",
            items = ranges,
            selectedItem = ranges.firstOrNull { it.second == mapJumpRangeState.distanceLy } ?: ranges.first(),
            onItemSelected = { onJumpRangeDistanceUpdate(it.second) },
            getItemName = { it.first },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanetsPanel(
    mapPlanetsState: MapPlanetsState,
    onBack: () -> Unit,
    onPlanetTypesUpdate: (List<PlanetType>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = "Planet types",
            onBack = onBack,
        )
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            PlanetTypes.types.forEach { type ->
                val isSelected = type in mapPlanetsState.selectedTypes
                RiftPill(
                    text = type.name,
                    icon = type.icon,
                    isSelected = isSelected,
                    onClick = {
                        val new = if (isSelected) {
                            mapPlanetsState.selectedTypes - type
                        } else {
                            mapPlanetsState.selectedTypes + type
                        }
                        onPlanetTypesUpdate(new)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsPanelTitle(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.onClick { onBack() },
    ) {
        RiftImageButton(
            resource = Res.drawable.backicon,
            size = 20.dp,
            onClick = onBack,
        )
        Text(
            text = title,
            style = RiftTheme.typography.titlePrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemColorPills(
    isExpanded: Boolean,
    isCellColor: Boolean,
    selected: MapSystemInfoType?,
    onPillClick: (MapSystemInfoType?) -> Unit,
    onPillEditClick: (MapSystemInfoType?) -> Unit,
    onPillHover: (MapSystemInfoType?, Boolean) -> Unit = { _, _ -> },
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        val colorEntries = MapSystemInfoType.entries - listOf(MapSystemInfoType.Planets)
        val pills = if (isCellColor) colorEntries + null else colorEntries
        pills.filter { isExpanded || selected == it }
            .forEach { type ->
                val (text, tooltip) = getMapStarInfoTypeColorName(type)
                RiftTooltipArea(
                    text = tooltip,
                ) {
                    RiftPill(
                        text = text,
                        isSelected = isExpanded && selected == type,
                        onClick = {
                            onPillClick(type)
                        },
                        onEditClick = editableInfoTypes[type]?.let { { onPillEditClick(type) } },
                        onHoverChange = {
                            onPillHover(type, it)
                        },
                    )
                }
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemIndicatorsPills(
    hidden: Set<MapSystemInfoType>,
    getInfoTypeNames: (color: MapSystemInfoType?) -> Pair<String, String>,
    selected: List<MapSystemInfoType>,
    onPillClick: (MapSystemInfoType) -> Unit,
    onPillEditClick: (MapSystemInfoType) -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier,
    ) {
        val pills = MapSystemInfoType.entries - hidden
        pills.forEach { type ->
            val (text, tooltip) = getInfoTypeNames(type)
            RiftTooltipArea(
                text = tooltip,
            ) {
                RiftPill(
                    text = text,
                    isSelected = type in selected,
                    onClick = {
                        onPillClick(type)
                    },
                    onEditClick = editableInfoTypes[type]?.let { { onPillEditClick(type) } },
                )
            }
        }
    }
}

/**
 * System coloring
 */
private fun getMapStarInfoTypeColorName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "Actual color" to "Colored with the\nactual color of the star"
        MapSystemInfoType.Security -> "Security Status" to "Colored according to the\nsecurity status"
        MapSystemInfoType.NullSecurity -> "Null-Sec Status" to "Colored according to the\nnegative security status"
        MapSystemInfoType.IntelHostiles -> "Hostiles count" to "Colored according to the\nnumber of reported hostiles"
        MapSystemInfoType.Jumps -> "Jumps" to "Colored according to the\nnumber of jumps in the last hour"
        MapSystemInfoType.Kills -> "Kills" to "Colored according to the\nnumber of ship and pod kills in the last hour"
        MapSystemInfoType.NpcKills -> "NPC Kills" to "Colored according to the\nnumber of NPCs killed in the last hour"
        MapSystemInfoType.Assets -> "Assets" to "Colored according to the\nnumber of owned assets located here"
        MapSystemInfoType.Incursions -> "Incursions" to "Colored according to the\nincursion status"
        MapSystemInfoType.Stations -> "Stations" to "Colored according to the\nnumber of stations"
        MapSystemInfoType.FactionWarfare -> "Faction Warfare" to "Colored according to the\nfaction warfare occupier"
        MapSystemInfoType.Sovereignty -> "Sovereignty" to "Colored according to the\nsovereignty holder"
        MapSystemInfoType.MetaliminalStorms -> "Metaliminal Storms" to "Colored according to the\npresence of metaliminal storms"
        MapSystemInfoType.JumpRange -> "Jump Range" to "Colored according to\njump range"
        MapSystemInfoType.Planets -> throw IllegalArgumentException("Not used for colors")
        MapSystemInfoType.JoveObservatories -> "Jove Observatories" to "Colored when a\nJove Observatory is present"
        MapSystemInfoType.Colonies -> "PI Colonies" to "Colored when you have a\nPI colony present"
        MapSystemInfoType.Clones -> "Clones" to "Colored when you have\njump clones present"
        null -> "None" to "No background color"
    }
}

/**
 * System indicators
 */
private fun getMapStarInfoTypeIndicatorName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> "Security status" to "Security status of the system"
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> "Jumps" to "Number of jumps in the last hour"
        MapSystemInfoType.Kills -> "Kills" to "Number of ship and pod kills in the last hour"
        MapSystemInfoType.NpcKills -> "NPC Kills" to "Number of NPCs killed in the last hour"
        MapSystemInfoType.Assets -> "Assets" to "Number of owned assets located here"
        MapSystemInfoType.Incursions -> "Incursions" to "Indicator for systems with an incursion"
        MapSystemInfoType.Stations -> "Stations" to "Number of stations"
        MapSystemInfoType.FactionWarfare -> "" to ""
        MapSystemInfoType.Sovereignty -> "Sovereignty" to "Sovereignty holder logo"
        MapSystemInfoType.MetaliminalStorms -> "Metaliminal Storms" to "Indicator for systems with a storm"
        MapSystemInfoType.JumpRange -> "Jump Range" to "Indicator for systems in jump range"
        MapSystemInfoType.Planets -> "Planets" to "Indicators for planets"
        MapSystemInfoType.JoveObservatories -> "Jove Observatories" to "Indicators for Jove Observatories"
        MapSystemInfoType.Colonies -> "PI Colonies" to "Indicators for PI colonies"
        MapSystemInfoType.Clones -> "Clones" to "Indicators for jump clones"
        null -> "None" to "No background color"
    }
}

/**
 * System info box indicators
 */
private fun getMapStarInfoTypeInfoBoxName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> "Security status" to "Security status of the system"
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> "Jumps" to "Number of jumps in the last hour"
        MapSystemInfoType.Kills -> "Kills" to "Number of ship and pod kills in the last hour"
        MapSystemInfoType.NpcKills -> "NPC Kills" to "Number of NPCs killed in the last hour"
        MapSystemInfoType.Assets -> "Assets" to "Number of owned assets located here"
        MapSystemInfoType.Incursions -> "Incursions" to "Incursion status"
        MapSystemInfoType.Stations -> "Stations" to "Number of stations"
        MapSystemInfoType.FactionWarfare -> "Faction Warfare" to "Faction warfare details"
        MapSystemInfoType.Sovereignty -> "Sovereignty" to "Sovereignty holder"
        MapSystemInfoType.MetaliminalStorms -> "Metaliminal Storms" to "Metaliminal storm type"
        MapSystemInfoType.JumpRange -> "Jump Range" to "Jump distance to system"
        MapSystemInfoType.Planets -> "Planets" to "Planets information"
        MapSystemInfoType.JoveObservatories -> "Jove Observatories" to "Jove Observatory presence information"
        MapSystemInfoType.Colonies -> "PI Colonies" to "PI colonies information"
        MapSystemInfoType.Clones -> "Clones" to "Jump clones information"
        null -> "None" to "No background color"
    }
}
