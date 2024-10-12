package dev.nohus.rift.map

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.ClickablePlayer
import dev.nohus.rift.compose.IntelTimer
import dev.nohus.rift.compose.LocalNow
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SystemEntities
import dev.nohus.rift.compose.SystemEntityInfoRow
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.indicator_assets
import dev.nohus.rift.generated.resources.indicator_clones
import dev.nohus.rift.generated.resources.indicator_colony
import dev.nohus.rift.generated.resources.indicator_incursion
import dev.nohus.rift.generated.resources.indicator_jove
import dev.nohus.rift.generated.resources.indicator_jump_drive
import dev.nohus.rift.generated.resources.indicator_jump_drive_no
import dev.nohus.rift.generated.resources.indicator_jumps
import dev.nohus.rift.generated.resources.indicator_kills
import dev.nohus.rift.generated.resources.indicator_npc_kills
import dev.nohus.rift.generated.resources.indicator_pod
import dev.nohus.rift.generated.resources.indicator_stations
import dev.nohus.rift.generated.resources.indicator_storm
import dev.nohus.rift.intel.state.IntelStateController.Dated
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase
import dev.nohus.rift.network.esi.SovereigntySystem
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import dev.nohus.rift.repositories.NamesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.roundSecurity
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemInfoBox(
    system: MapSolarSystem,
    regionName: String?,
    isHighlightedOrHovered: Boolean,
    intel: List<Dated<SystemEntity>>?,
    hasIntelPopup: Boolean,
    onlineCharacters: List<GetOnlineCharactersLocationUseCase.OnlineCharacterLocation>,
    systemStatus: SolarSystemStatus?,
    infoTypes: List<MapSystemInfoType>,
    indicatorsInfoTypes: List<MapSystemInfoType>,
    onRegionClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        val isExpanded = hasIntelPopup || isHighlightedOrHovered
        val intelInPopup = if (isExpanded) intel else null
        val borderColor = if (isHighlightedOrHovered) RiftTheme.colors.borderGreyLight else RiftTheme.colors.borderGrey
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.8f))
                .border(1.dp, borderColor)
                .padding(horizontal = 2.dp, vertical = 1.dp),
        ) {
            ScrollbarColumn(
                isScrollbarConditional = true,
                isFillWidth = false,
                modifier = Modifier.width(IntrinsicSize.Max),
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                ) {
                    val intelGroups = if (intelInPopup != null) groupIntelByTime(intelInPopup) else null
                    Row {
                        val isShowingSecurity = (isExpanded && MapSystemInfoType.Security in infoTypes) || (!isExpanded && MapSystemInfoType.Security in indicatorsInfoTypes)
                        val securityColor = SecurityColors[system.security]
                        val systemNameText = buildAnnotatedString {
                            append(system.name)
                            if (isShowingSecurity) {
                                append(" ")
                                withStyle(SpanStyle(color = securityColor)) {
                                    append(system.security.roundSecurity().toString())
                                }
                            }
                        }
                        val systemNameStyle = RiftTheme.typography.captionBoldPrimary
                        val highlightedSystemNameStyle =
                            RiftTheme.typography.bodyHighlighted.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val style = if (isHighlightedOrHovered) highlightedSystemNameStyle else systemNameStyle
                        Text(
                            text = systemNameText,
                            style = style,
                        )
                    }

                    if (regionName != null) {
                        Text(
                            text = regionName,
                            style = RiftTheme.typography.captionSecondary,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                .onClick { onRegionClick() },
                        )
                    }

                    if (isExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            modifier = Modifier.modifyIf(intelGroups != null) { padding(bottom = 1.dp) },
                        ) {
                            SystemInfoTypes(system, infoTypes, systemStatus)
                            onlineCharacters.forEach { onlineCharacterLocation ->
                                ClickablePlayer(onlineCharacterLocation.id) {
                                    SystemEntityInfoRow(32.dp, hasBorder = false) {
                                        AsyncPlayerPortrait(
                                            characterId = onlineCharacterLocation.id,
                                            size = 32,
                                            modifier = Modifier.size(32.dp),
                                        )
                                        Text(
                                            text = onlineCharacterLocation.name,
                                            style = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.onlineGreen),
                                            modifier = Modifier.padding(4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (intelGroups != null) {
                        Divider(
                            color = RiftTheme.colors.divider,
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        )
                        Intel(intelGroups, system)
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 2.dp, top = Spacing.verySmall),
        ) {
            val shownIndicatorsInfoTypes = if (isExpanded) indicatorsInfoTypes - infoTypes.toSet() else indicatorsInfoTypes
            SystemInfoTypesIndicators(system, shownIndicatorsInfoTypes, systemStatus)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.SystemInfoTypes(
    system: MapSolarSystem,
    infoTypes: List<MapSystemInfoType>,
    systemStatus: SolarSystemStatus?,
) {
    val namesRepository: NamesRepository = koin.get()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        infoTypes.distinct()
            .sortedByDescending { listOf(MapSystemInfoType.Sovereignty).indexOf(it) }
            .forEach { color ->
                when (color) {
                    MapSystemInfoType.StarColor -> {}
                    MapSystemInfoType.Security -> {}
                    MapSystemInfoType.NullSecurity -> {}
                    MapSystemInfoType.IntelHostiles -> {}
                    MapSystemInfoType.Jumps -> {
                        val jumps = systemStatus?.shipJumps?.takeIf { it > 0 }?.toString()
                        InfoTypeIndicator(jumps, Res.drawable.indicator_jumps, "Jumps: $jumps")
                    }
                    MapSystemInfoType.Kills -> {
                        val podKills = systemStatus?.podKills?.takeIf { it > 0 }?.toString()
                        InfoTypeIndicator(podKills, Res.drawable.indicator_pod, "Pod kills: $podKills")
                        val shipKills = systemStatus?.shipKills?.takeIf { it > 0 }?.toString()
                        InfoTypeIndicator(shipKills, Res.drawable.indicator_kills, "Ship kills: $shipKills")
                    }
                    MapSystemInfoType.NpcKills -> {
                        val npcKills = systemStatus?.npcKills?.takeIf { it > 0 }?.toString()
                        InfoTypeIndicator(npcKills, Res.drawable.indicator_npc_kills, "NPC kills: $npcKills")
                    }
                    MapSystemInfoType.Assets -> {
                        val assets = systemStatus?.assetCount?.takeIf { it > 0 }?.toString()
                        InfoTypeIndicator(assets, Res.drawable.indicator_assets, "Assets: $assets")
                    }
                    MapSystemInfoType.Incursions -> {} // In column
                    MapSystemInfoType.Stations -> {
                        val stations = systemStatus?.stations?.size?.takeIf { it > 0 }?.toString()
                        InfoTypeIndicator(stations, Res.drawable.indicator_stations, "Stations: $stations")
                    }
                    MapSystemInfoType.FactionWarfare -> {} // In column
                    MapSystemInfoType.Sovereignty -> {} // In column
                    MapSystemInfoType.MetaliminalStorms -> {} // In column
                    MapSystemInfoType.Planets -> {} // In column
                    MapSystemInfoType.JoveObservatories -> {
                        InfoTypeIndicator("".takeIf { system.hasJoveObservatory }, Res.drawable.indicator_jove, "Jove Observatory")
                    }
                    MapSystemInfoType.JumpRange -> {
                        systemStatus?.distance?.let {
                            val lightYears = String.format("%.1fly", it.distanceLy)
                            if (it.isInJumpRange) {
                                InfoTypeIndicator(lightYears, Res.drawable.indicator_jump_drive, "Jump distance - in range")
                            } else {
                                InfoTypeIndicator(lightYears, Res.drawable.indicator_jump_drive_no, "Jump distance - out of range")
                            }
                        }
                    }
                    MapSystemInfoType.Colonies -> {
                        val colonies = systemStatus?.colonies?.takeIf { it > 0 }
                        InfoTypeIndicator("$colonies".takeIf { colonies != null }, Res.drawable.indicator_colony, "Colonies: $colonies")
                    }
                    MapSystemInfoType.Clones -> {} // In column
                }
            }
    }
    infoTypes.distinct()
        .sortedBy { listOf(MapSystemInfoType.Sovereignty).indexOf(it) }
        .forEach { color ->
            when (color) {
                MapSystemInfoType.StarColor -> {}
                MapSystemInfoType.Security -> {}
                MapSystemInfoType.NullSecurity -> {}
                MapSystemInfoType.IntelHostiles -> {}
                MapSystemInfoType.Jumps -> {} // In icon row
                MapSystemInfoType.Kills -> {} // In icon row
                MapSystemInfoType.NpcKills -> {} // In icon row
                MapSystemInfoType.Assets -> {} // In icon row
                MapSystemInfoType.Incursions -> {
                    systemStatus?.incursion?.let { incursion ->
                        Text(
                            text = "${incursion.type}: ${incursion.state.name}",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                }
                MapSystemInfoType.Stations -> {} // In icon row
                MapSystemInfoType.FactionWarfare -> {
                    systemStatus?.factionWarfare?.let { factionWarfare ->
                        val owner = namesRepository.getName(factionWarfare.ownerFactionId) ?: "Unknown"
                        val occupier = namesRepository.getName(factionWarfare.occupierFactionId) ?: "Unknown"
                        val text = buildString {
                            appendLine("Faction warfare: ${factionWarfare.contested.name}")
                            appendLine("Owner: $owner")
                            if (occupier != owner) {
                                appendLine("Occupier: $occupier")
                            }
                            if (factionWarfare.victoryPoints != 0) {
                                appendLine("Points: ${factionWarfare.victoryPoints}/${factionWarfare.victoryPointsThreshold}")
                            }
                        }.trim()
                        Text(
                            text = text,
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                }
                MapSystemInfoType.Sovereignty -> {
                    systemStatus?.sovereignty?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val id = it.allianceId ?: it.factionId ?: it.corporationId
                            if (id != null) {
                                SovereigntyLogo(it)
                                val name = namesRepository.getName(id) ?: "Unknown"
                                Text(
                                    text = name,
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            }
                        }
                    }
                }
                MapSystemInfoType.MetaliminalStorms -> {
                    systemStatus?.storms?.let {
                        it.forEach { storm ->
                            Text(
                                text = "Storm: ${storm.strength.name} ${storm.type.name}",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }
                }
                MapSystemInfoType.JumpRange -> {} // In icon row
                MapSystemInfoType.Planets -> {
                    systemStatus?.planets?.let {
                        FlowRow(
                            maxItemsInEachRow = 5,
                        ) {
                            it.sortedWith(compareBy({ it.type.typeId }, { it.name })).forEach { planet ->
                                RiftTooltipArea(
                                    text = "${planet.name} – ${planet.type.name}",
                                ) {
                                    Image(
                                        painter = painterResource(planet.type.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                MapSystemInfoType.JoveObservatories -> {} // In icon row
                MapSystemInfoType.Colonies -> {} // In icon row
                MapSystemInfoType.Clones -> {
                    val clones = systemStatus?.clones?.takeIf { it.isNotEmpty() }
                    if (clones != null) {
                        ClonesIndicators(clones, true)
                    }
                }
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemInfoTypesIndicators(
    system: MapSolarSystem,
    infoTypes: List<MapSystemInfoType>,
    systemStatus: SolarSystemStatus?,
) {
    infoTypes.distinct()
        .sortedBy { listOf(MapSystemInfoType.Incursions, MapSystemInfoType.Sovereignty).indexOf(it) }
        .forEach { color ->
            when (color) {
                MapSystemInfoType.StarColor -> {}
                MapSystemInfoType.Security -> {}
                MapSystemInfoType.NullSecurity -> {}
                MapSystemInfoType.IntelHostiles -> {}
                MapSystemInfoType.Jumps -> {
                    val jumps = systemStatus?.shipJumps?.takeIf { it > 0 }?.toString()
                    InfoTypeIndicator(jumps, Res.drawable.indicator_jumps)
                }
                MapSystemInfoType.Kills -> {
                    val podKills = systemStatus?.podKills ?: 0
                    val shipKills = systemStatus?.shipKills ?: 0
                    val kills = (podKills + shipKills).takeIf { it > 0 }?.toString()
                    InfoTypeIndicator(kills, Res.drawable.indicator_kills)
                }

                MapSystemInfoType.NpcKills -> {
                    val npcKills = systemStatus?.npcKills?.takeIf { it > 0 }?.toString()
                    InfoTypeIndicator(npcKills, Res.drawable.indicator_npc_kills)
                }

                MapSystemInfoType.Assets -> {
                    val assets = systemStatus?.assetCount?.takeIf { it > 0 }?.toString()
                    InfoTypeIndicator(assets, Res.drawable.indicator_assets)
                }
                MapSystemInfoType.Incursions -> {
                    systemStatus?.incursion?.let {
                        InfoTypeIndicator("", Res.drawable.indicator_incursion)
                    }
                }
                MapSystemInfoType.Stations -> {
                    val stations = systemStatus?.stations?.size?.takeIf { it > 0 }?.toString()
                    InfoTypeIndicator(stations, Res.drawable.indicator_stations)
                }
                MapSystemInfoType.FactionWarfare -> {}
                MapSystemInfoType.Sovereignty -> {
                    systemStatus?.sovereignty?.let {
                        SovereigntyLogo(it)
                    }
                }
                MapSystemInfoType.MetaliminalStorms -> {
                    systemStatus?.storms?.takeIf { it.isNotEmpty() }?.let {
                        InfoTypeIndicator("", Res.drawable.indicator_storm)
                    }
                }
                MapSystemInfoType.JumpRange ->
                    systemStatus?.distance?.let {
                        if (it.isInJumpRange) {
                            val lightYears = String.format("%.1fly", it.distanceLy)
                            InfoTypeIndicator(lightYears, Res.drawable.indicator_jump_drive)
                        }
                    }
                MapSystemInfoType.Planets -> {
                    systemStatus?.planets?.let {
                        FlowRow(
                            maxItemsInEachRow = 5,
                        ) {
                            it.sortedWith(compareBy({ it.type.typeId }, { it.name })).forEach { planet ->
                                RiftTooltipArea(
                                    text = "${planet.name} – ${planet.type.name}",
                                ) {
                                    Image(
                                        painter = painterResource(planet.type.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                MapSystemInfoType.JoveObservatories -> {
                    if (system.hasJoveObservatory) {
                        InfoTypeIndicator("", Res.drawable.indicator_jove)
                    }
                }
                MapSystemInfoType.Colonies -> {
                    val colonies = systemStatus?.colonies?.takeIf { it > 0 }
                    if (colonies != null) {
                        InfoTypeIndicator("$colonies", Res.drawable.indicator_colony)
                    }
                }
                MapSystemInfoType.Clones -> {
                    val clones = systemStatus?.clones?.takeIf { it.isNotEmpty() }
                    if (clones != null) {
                        ClonesIndicators(clones, false)
                    }
                }
            }
        }
}

@Composable
private fun ClonesIndicators(clones: Map<Int, Int>, withDetails: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.verySmall),
    ) {
        Image(
            painter = painterResource(Res.drawable.indicator_clones),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        clones?.forEach { (characterId, count) ->
            RiftTooltipArea(
                text = "$count clone${count.plural}".takeIf { withDetails },
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
                ) {
                    AsyncPlayerPortrait(
                        characterId = characterId,
                        size = 32,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (withDetails) {
            val totalCount = clones?.entries?.sumOf { it.value } ?: 0
            Text(
                text = "$totalCount jump clone${totalCount.plural}",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
}

@Composable
private fun SovereigntyLogo(sovereignty: SovereigntySystem) {
    val requestSize = 32
    val size = 24
    if (sovereignty.allianceId != null) {
        AsyncAllianceLogo(
            allianceId = sovereignty.allianceId,
            size = requestSize,
            modifier = Modifier.size(size.dp),
        )
    } else if (sovereignty.factionId != null || sovereignty.corporationId != null) {
        AsyncCorporationLogo(
            corporationId = sovereignty.factionId ?: sovereignty.corporationId,
            size = requestSize,
            modifier = Modifier.size(size.dp),
        )
    }
}

@Composable
private fun InfoTypeIndicator(
    text: String?,
    icon: DrawableResource,
    tooltip: String? = null,
) {
    if (text == null) return
    val content = movableContentOf {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.verySmall),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
    if (tooltip != null) {
        RiftTooltipArea(
            text = tooltip,
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun Intel(
    groups: Map<Instant, List<SystemEntity>>,
    system: MapSolarSystem,
) {
    CompositionLocalProvider(LocalNow provides getNow()) {
        Column {
            val isCompact = groups.hasAtLeast(8)
            for ((index, group) in groups.entries.sortedByDescending { it.key }.withIndex()) {
                if (index > 0) {
                    Divider(
                        color = RiftTheme.colors.divider,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val entities = group.value
                    IntelTimer(
                        timestamp = group.key,
                        style = RiftTheme.typography.captionBoldPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    SystemEntities(
                        entities = entities,
                        system = system.name,
                        rowHeight = if (isCompact) 24.dp else 32.dp,
                        isGroupingCharacters = isCompact,
                    )
                }
            }
        }
    }
}

private fun <T1, T2> Map<T1, List<T2>>.hasAtLeast(count: Int): Boolean {
    var sum = 0
    for (entry in entries) {
        sum += entry.value.size
        if (sum >= count) return true
    }
    return false
}

fun groupIntelByTime(intel: List<Dated<SystemEntity>>): Map<Instant, List<SystemEntity>> {
    // Group entities by when they were reported, so they can be displayed with a single timer by group
    val groups = mutableMapOf<Instant, List<SystemEntity>>()
    intel.forEach { item ->
        val group = groups.keys.firstOrNull { Duration.between(item.timestamp, it).abs() < Duration.ofSeconds(10) }
        if (group != null) {
            groups[group] = groups.getValue(group) + item.item
        } else {
            groups[item.timestamp] = listOf(item.item)
        }
    }
    return groups
}
