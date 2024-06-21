package dev.nohus.rift.map

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.ClickablePlayer
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SystemEntities
import dev.nohus.rift.compose.SystemEntityInfoRow
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.indicator_assets
import dev.nohus.rift.generated.resources.indicator_incursion
import dev.nohus.rift.generated.resources.indicator_jump_drive
import dev.nohus.rift.generated.resources.indicator_jumps
import dev.nohus.rift.generated.resources.indicator_kills
import dev.nohus.rift.generated.resources.indicator_npc_kills
import dev.nohus.rift.generated.resources.indicator_stations
import dev.nohus.rift.generated.resources.indicator_storm
import dev.nohus.rift.intel.state.IntelStateController.Dated
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import dev.nohus.rift.repositories.NamesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import dev.nohus.rift.utils.roundSecurity
import kotlinx.coroutines.delay
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
            Column {
                val intelGroups = if (intelInPopup != null) groupIntelByTime(intelInPopup) else null
                Row {
                    val isShowingSecurity = (isExpanded && MapSystemInfoType.Security in infoTypes) || MapSystemInfoType.Security in indicatorsInfoTypes
                    val isShowingIntelTimer = intelGroups?.size == 1
                    val securityColor = SecurityColors[system.security]
                    val systemNameText = buildAnnotatedString {
                        append(system.name)
                        if (isShowingSecurity) {
                            append(" ")
                            withStyle(SpanStyle(color = securityColor)) {
                                append(system.security.roundSecurity().toString())
                            }
                        }
                        if (isShowingIntelTimer) append(" ")
                    }
                    val systemNameStyle = RiftTheme.typography.captionBoldPrimary
                    val highlightedSystemNameStyle =
                        RiftTheme.typography.bodyHighlighted.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val style = if (isHighlightedOrHovered) highlightedSystemNameStyle else systemNameStyle
                    Text(
                        text = systemNameText,
                        style = style,
                    )
                    if (isShowingIntelTimer) {
                        Timer(intelGroups!!.keys.first())
                    }
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
                        onlineCharacters.forEach { onlineCharacterLocation ->
                            ClickablePlayer(onlineCharacterLocation.id) {
                                SystemEntityInfoRow {
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
                        SystemInfoTypes(infoTypes, systemStatus)
                    }
                }

                if (intelGroups != null) {
                    Intel(intelGroups, system)
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 2.dp, top = Spacing.verySmall),
        ) {
            val shownIndicatorsInfoTypes = if (isExpanded) indicatorsInfoTypes - infoTypes.toSet() else indicatorsInfoTypes
            SystemInfoTypesIndicators(shownIndicatorsInfoTypes, systemStatus)
        }
    }
}

@Composable
private fun ColumnScope.SystemInfoTypes(
    infoTypes: List<MapSystemInfoType>,
    systemStatus: SolarSystemStatus?,
) {
    val namesRepository: NamesRepository = koin.get()
    infoTypes.distinct().forEach { color ->
        when (color) {
            MapSystemInfoType.StarColor -> {}
            MapSystemInfoType.Security -> {}
            MapSystemInfoType.IntelHostiles -> {}
            MapSystemInfoType.Jumps -> {
                val jumps = systemStatus?.shipJumps ?: 0
                if (jumps > 0) {
                    Text(
                        text = "Jumps: $jumps",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }

            MapSystemInfoType.Kills -> {
                val podKills = systemStatus?.podKills ?: 0
                val shipKills = systemStatus?.shipKills ?: 0
                if (podKills > 0 || shipKills > 0) {
                    Text(
                        text = "Pod kills: $podKills\nShip kills: $shipKills",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }

            MapSystemInfoType.NpcKills -> {
                val npcKills = systemStatus?.npcKills ?: 0
                if (npcKills > 0) {
                    Text(
                        text = "NPC kills: $npcKills",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }

            MapSystemInfoType.Assets -> {
                val assets = systemStatus?.assetCount ?: 0
                if (assets > 0) {
                    Text(
                        text = "Assets: $assets",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }

            MapSystemInfoType.Incursions -> {
                systemStatus?.incursion?.let { incursion ->
                    Text(
                        text = "${incursion.type}: ${incursion.state.name}",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }
            MapSystemInfoType.Stations -> {
                systemStatus?.stations?.takeIf { it.isNotEmpty() }?.let { stations ->
                    Text(
                        text = "Stations: ${stations.size}",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }
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
                    val id = it.allianceId ?: it.factionId ?: it.corporationId
                    if (id != null) {
                        val name = namesRepository.getName(id) ?: "Unknown"
                        Text(
                            text = name,
                            style = RiftTheme.typography.bodyPrimary,
                        )
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

            MapSystemInfoType.JumpRange -> {
                systemStatus?.distance?.let {
                    val lightYears = String.format("%.1fly", it.distanceLy)
                    val text = buildString {
                        append(lightYears)
                        if (it.isInJumpRange) append(" – in range")
                    }
                    Text(
                        text = text,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemInfoTypesIndicators(
    infoTypes: List<MapSystemInfoType>,
    systemStatus: SolarSystemStatus?,
) {
    infoTypes.distinct().forEach { color ->
        when (color) {
            MapSystemInfoType.StarColor -> {}
            MapSystemInfoType.Security -> {}
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
            MapSystemInfoType.Sovereignty -> {}
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
        }
    }
}

@Composable
private fun InfoTypeIndicator(
    text: String?,
    icon: DrawableResource,
) {
    if (text == null) return
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

@Composable
private fun Intel(
    groups: Map<Instant, List<SystemEntity>>,
    system: MapSolarSystem,
) {
    ScrollbarColumn(
        isScrollbarConditional = true,
        isFillWidth = false,
        modifier = Modifier.heightIn(max = 300.dp),
    ) {
        for (group in groups.entries.sortedByDescending { it.key }) {
            val entities = group.value
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (groups.size > 1) {
                    Timer(group.key)
                }
                SystemEntities(entities, system.name)
            }
        }
    }
}

private fun groupIntelByTime(intel: List<Dated<SystemEntity>>): Map<Instant, List<SystemEntity>> {
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

@Composable
private fun Timer(
    timestamp: Instant,
    modifier: Modifier = Modifier,
) {
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            delay(300)
            value = Instant.now()
        }
    }
    val duration = Duration.between(timestamp, now)
    val colorFadePercentage = (duration.toSeconds() / 120f).coerceIn(0f, 1f)
    val color = lerp(RiftTheme.colors.textSpecialHighlighted, RiftTheme.colors.textSecondary, colorFadePercentage)
    Text(
        text = formatDuration(duration),
        style = RiftTheme.typography.captionBoldPrimary.copy(color = color),
        modifier = modifier,
    )
}

private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes < 10) {
        val seconds = duration.toSecondsPart()
        String.format("%d:%02d", minutes, seconds)
    } else if (minutes < 60) {
        "${minutes}m"
    } else {
        val hours = duration.toHours()
        "${hours}h"
    }
}
