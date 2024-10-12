package dev.nohus.rift.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.keywords_combat_probe
import dev.nohus.rift.generated.resources.keywords_ess
import dev.nohus.rift.generated.resources.keywords_gatecamp
import dev.nohus.rift.generated.resources.keywords_interdiction_probe
import dev.nohus.rift.generated.resources.keywords_killreport
import dev.nohus.rift.generated.resources.keywords_no_visual
import dev.nohus.rift.generated.resources.keywords_skyhook
import dev.nohus.rift.generated.resources.keywords_spike
import dev.nohus.rift.generated.resources.keywords_wormhole
import dev.nohus.rift.intel.state.CharacterBound
import dev.nohus.rift.intel.state.Clearable
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.repositories.CharacterDetailsRepository.CharacterDetails
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.standings.getColor
import dev.nohus.rift.standings.isFriendly
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.toURIOrNull
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SystemEntities(
    entities: List<SystemEntity>,
    system: String,
    rowHeight: Dp,
    isHorizontal: Boolean = false,
    isGroupingCharacters: Boolean = false,
) {
    entities.filterIsInstance<SystemEntity.Killmail>().forEach { killmail ->
        SystemEntityInfoRow(rowHeight, isHorizontal) {
            ClickableEntity(
                onClick = { killmail.url.toURIOrNull()?.openBrowser() },
            ) {
                Image(
                    painter = painterResource(Res.drawable.keywords_killreport),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
            }
            if (killmail.ship != null) {
                val repository: ShipTypesRepository by koin.inject()
                val shipTypeId = repository.getShipTypeId(killmail.ship)
                ClickableShip(killmail.ship, shipTypeId) {
                    RiftTooltipArea(
                        text = killmail.ship,
                    ) {
                        AsyncTypeIcon(
                            typeId = shipTypeId,
                            modifier = Modifier.size(rowHeight),
                        )
                    }
                }
            } else if (killmail.typeName != null) {
                val typedRepository: TypesRepository by koin.inject()
                val type = typedRepository.getType(killmail.typeName)
                RiftTooltipArea(
                    text = killmail.typeName,
                ) {
                    AsyncTypeIcon(
                        type = type,
                        modifier = Modifier.size(rowHeight),
                    )
                }
            }
            if (killmail.victim.characterId != null && killmail.victim.details != null) {
                CharactersPortraits(listOf(killmail.victim.details), rowHeight)
            } else {
                // No character (killmail of a deployable, etc.), show just corporation/alliance
                CharacterMembership(
                    corporationId = killmail.victim.corporationId,
                    corporationName = killmail.victim.corporationName,
                    allianceId = killmail.victim.allianceId,
                    allianceName = killmail.victim.allianceName,
                    rowHeight = rowHeight,
                )
            }

            var style = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
            killmail.victim.standing?.getColor()?.let { style = style.copy(color = it) }
            val ticket = killmail.victim.allianceTicker ?: if (killmail.victim.corporationId?.isNpcCorp() == true) "NPC Corp" else killmail.victim.corporationTicker ?: ""
            if (rowHeight < 32.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(horizontal = Spacing.small),
                ) {
                    Text(
                        text = ticket,
                        style = RiftTheme.typography.bodySecondary,
                    )
                    Text(
                        text = if (killmail.victim.standing?.isFriendly == true) "Loss" else "Kill",
                        style = style,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.small),
                ) {
                    Text(
                        text = if (killmail.victim.standing?.isFriendly == true) "Loss" else "Kill",
                        style = style,
                    )
                    Text(
                        text = ticket,
                        style = RiftTheme.typography.bodySecondary,
                    )
                }
            }
        }
    }
    entities.filterIsInstance<SystemEntity.Ship>().forEach { ship ->
        val repository: ShipTypesRepository by koin.inject()
        val shipTypeId = repository.getShipTypeId(ship.name)
        ClickableShip(ship.name, shipTypeId) {
            SystemEntityInfoRow(rowHeight, isHorizontal) {
                AsyncTypeIcon(
                    typeId = shipTypeId,
                    modifier = Modifier.size(rowHeight),
                )

                var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
                ship.standing?.getColor()?.let { nameStyle = nameStyle.copy(color = it) }

                val text = if (ship.count > 1) {
                    "${ship.count}x ${ship.name}"
                } else {
                    ship.name
                }
                Text(
                    text = text,
                    style = nameStyle,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
    if (isGroupingCharacters) {
        entities.filterIsInstance<SystemEntity.Character>()
            .groupBy {
                it.details.allianceId ?: if (it.details.corporationId.isNpcCorp()) 1 else it.details.corporationId
            }
            .forEach { (_, characters) ->
                SystemEntityInfoRow(rowHeight, isHorizontal) {
                    CharactersPortraits(characters.map { it.details }, rowHeight)

                    val representative = characters.first().details
                    val characterWord = if (representative.standing.isFriendly) "friendly" else "hostile${characters.size.plural}"
                    var style = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
                    representative.standing.getColor()?.let { style = style.copy(color = it) }
                    val ticker = representative.allianceTicker ?: if (representative.corporationId.isNpcCorp()) "NPC Corp" else representative.corporationTicker ?: ""
                    if (rowHeight < 32.dp) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier.padding(horizontal = Spacing.small),
                        ) {
                            Text(
                                text = ticker,
                                style = RiftTheme.typography.bodySecondary,
                            )
                            Text(
                                text = "${characters.size} $characterWord",
                                style = style,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = "${characters.size} $characterWord",
                                style = style,
                            )
                            Text(
                                text = ticker,
                                style = RiftTheme.typography.bodySecondary,
                            )
                        }
                    }
                }
            }
    } else {
        entities.filterIsInstance<SystemEntity.Character>()
            .sortedWith(compareBy({ it.details.allianceId }, { it.details.corporationId }))
            .forEach { character ->
                ClickablePlayer(character.characterId) {
                    SystemEntityInfoRow(rowHeight, isHorizontal) {
                        AsyncPlayerPortrait(
                            characterId = character.characterId,
                            size = 32,
                            modifier = Modifier.size(rowHeight),
                        )
                        RiftTooltipArea(
                            text = character.details.corporationName ?: "",
                        ) {
                            AsyncCorporationLogo(
                                corporationId = character.details.corporationId,
                                size = 32,
                                modifier = Modifier.size(rowHeight),
                            )
                        }
                        if (character.details.allianceId != null) {
                            RiftTooltipArea(
                                text = character.details.allianceName ?: "",
                            ) {
                                AsyncAllianceLogo(
                                    allianceId = character.details.allianceId,
                                    size = 32,
                                    modifier = Modifier.size(rowHeight),
                                )
                            }
                        }

                        val ticker = buildString {
                            character.details.corporationTicker?.let { append("$it ") }
                            character.details.allianceTicker?.let { append(it) }
                        }
                        var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
                        character.details.standing.getColor()?.let { nameStyle = nameStyle.copy(color = it) }
                        if (rowHeight < 32.dp) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier.padding(horizontal = Spacing.small),
                            ) {
                                Text(
                                    text = ticker,
                                    style = RiftTheme.typography.bodySecondary,
                                )
                                Text(
                                    text = character.name,
                                    style = nameStyle,
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                Text(
                                    text = character.name,
                                    style = nameStyle,
                                )
                                Text(
                                    text = ticker,
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            }
                        }
                    }
                }
            }
    }
    entities.firstOrNull { it is SystemEntity.UnspecifiedCharacter }?.let {
        val hasNamedHostiles =
            (entities.filterIsInstance<SystemEntity.Character>() + entities.filterIsInstance<SystemEntity.Ship>()).isNotEmpty()
        val unspecified = it as SystemEntity.UnspecifiedCharacter
        val content = @Composable {
            Text(
                text = "${if (hasNamedHostiles) "+" else ""}${unspecified.count} hostile${unspecified.count.plural}",
                style = RiftTheme.typography.bodyHighlighted,
                modifier = Modifier.padding(4.dp),
            )
        }
        if (isHorizontal) {
            SystemEntityInfoRow(rowHeight, true) {
                content()
            }
        } else {
            content()
        }
    }
    entities.forEach { entity ->
        when (entity) {
            SystemEntity.Bubbles -> IconInfoRow(Res.drawable.keywords_interdiction_probe, "Bubbles", rowHeight, isHorizontal)
            SystemEntity.CombatProbes -> IconInfoRow(Res.drawable.keywords_combat_probe, "Combat probes", rowHeight, isHorizontal)
            SystemEntity.Ess -> IconInfoRow(Res.drawable.keywords_ess, "ESS", rowHeight, isHorizontal)
            SystemEntity.Skyhook -> IconInfoRow(Res.drawable.keywords_skyhook, "Skyhook", rowHeight, isHorizontal)
            is SystemEntity.Gate -> GateInfoRow(system, entity, rowHeight, isHorizontal)
            SystemEntity.GateCamp -> IconInfoRow(Res.drawable.keywords_gatecamp, "Gate camp", rowHeight, isHorizontal)
            SystemEntity.NoVisual -> NoVisualRow(rowHeight, isHorizontal)
            SystemEntity.Spike -> IconInfoRow(Res.drawable.keywords_spike, "Spike", rowHeight, isHorizontal)
            SystemEntity.Wormhole -> WormholeInfoRow(rowHeight, isHorizontal)
            is SystemEntity.Character -> {}
            is SystemEntity.UnspecifiedCharacter -> {}
            is SystemEntity.Ship -> {}
            is SystemEntity.Killmail -> {}
            is CharacterBound -> {}
            is Clearable -> {}
        }
    }
}

@Composable
private fun CharactersPortraits(
    characters: List<CharacterDetails>,
    rowHeight: Dp,
) {
    Row {
        if (characters.size > 3) {
            InfiniteScrollingCarousel(
                items = characters,
                modifier = Modifier.height(rowHeight).width(rowHeight * 3),
            ) { character ->
                ClickablePlayer(character.characterId) {
                    RiftTooltipArea(
                        text = character.name,
                    ) {
                        AsyncPlayerPortrait(
                            characterId = character.characterId,
                            size = 32,
                            withAnimatedLoading = false,
                            modifier = Modifier.size(rowHeight),
                        )
                    }
                }
            }
        } else {
            Row {
                characters.forEach { character ->
                    ClickablePlayer(character.characterId) {
                        RiftTooltipArea(
                            text = character.name,
                        ) {
                            AsyncPlayerPortrait(
                                characterId = character.characterId,
                                size = 32,
                                modifier = Modifier.size(rowHeight),
                            )
                        }
                    }
                }
            }
        }
    }
    val representative = characters.first()
    CharacterMembership(
        corporationId = representative.corporationId,
        corporationName = representative.corporationName,
        allianceId = representative.allianceId,
        allianceName = representative.allianceName,
        rowHeight = rowHeight,
    )
}

/**
 * Logo of alliance or corporation (if no alliance)
 */
@Composable
private fun CharacterMembership(
    corporationId: Int?,
    corporationName: String?,
    allianceId: Int?,
    allianceName: String?,
    rowHeight: Dp,
) {
    if (allianceId != null) {
        ClickableAlliance(allianceId) {
            RiftTooltipArea(
                text = allianceName ?: "",
            ) {
                AsyncAllianceLogo(
                    allianceId = allianceId,
                    size = 32,
                    modifier = Modifier.size(rowHeight),
                )
            }
        }
    } else if (corporationId != null) {
        ClickableCorporation(corporationId) {
            RiftTooltipArea(
                text = corporationName ?: "",
            ) {
                AsyncCorporationLogo(
                    corporationId = corporationId,
                    size = 32,
                    modifier = Modifier.size(rowHeight),
                )
            }
        }
    }
}

@Composable
fun SystemEntityInfoRow(
    rowHeight: Dp,
    hasBorder: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val border = RiftTheme.colors.borderGreyLight
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .heightIn(min = rowHeight)
            .modifyIf(hasBorder) { Modifier.border(1.dp, border) },
        content = content,
    )
}

@Composable
private fun IconInfoRow(icon: DrawableResource, text: String, rowHeight: Dp, isHorizontal: Boolean) {
    SystemEntityInfoRow(rowHeight, isHorizontal) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(rowHeight),
        )
        Text(
            text = text,
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun WormholeInfoRow(rowHeight: Dp, isHorizontal: Boolean) {
    SystemEntityInfoRow(rowHeight, isHorizontal) {
        val transition = rememberInfiniteTransition()
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        )
        Image(
            painter = painterResource(Res.drawable.keywords_wormhole),
            contentDescription = null,
            modifier = Modifier
                .size(rowHeight)
                .rotate(-rotation),
        )
        Text(
            text = "Wormhole",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun GateInfoRow(system: String, entity: SystemEntity.Gate, rowHeight: Dp, isHorizontal: Boolean) {
    SystemEntityInfoRow(rowHeight, isHorizontal) {
        GateIcon(entity.isAnsiblex, system, entity.system, rowHeight)
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        val gateText = if (entity.isAnsiblex) "Ansiblex" else "Gate"
        Text(
            text = "${entity.system} $gateText",
            style = RiftTheme.typography.bodyHighlighted,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun NoVisualRow(
    rowHeight: Dp,
    isHorizontal: Boolean,
) {
    val content = @Composable {
        Image(
            painter = painterResource(Res.drawable.keywords_no_visual),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "No visual",
            style = RiftTheme.typography.bodyPrimary.copy(fontSize = 11.sp),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
    if (isHorizontal) {
        if (rowHeight < 32.dp) {
            SystemEntityInfoRow(rowHeight, true) {
                Spacer(Modifier.width(Spacing.verySmall))
                content()
            }
        } else {
            SystemEntityInfoRow(rowHeight, true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    content()
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = 16.dp),
        ) {
            content()
        }
    }
}

private fun Int.isNpcCorp(): Boolean = this in 1000001..1000441
