package dev.nohus.rift.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.keywords_combat_probe
import dev.nohus.rift.generated.resources.keywords_ess
import dev.nohus.rift.generated.resources.keywords_gatecamp
import dev.nohus.rift.generated.resources.keywords_interdiction_probe
import dev.nohus.rift.generated.resources.keywords_killreport
import dev.nohus.rift.generated.resources.keywords_no_visual
import dev.nohus.rift.generated.resources.keywords_spike
import dev.nohus.rift.generated.resources.keywords_wormhole
import dev.nohus.rift.intel.state.CharacterBound
import dev.nohus.rift.intel.state.Clearable
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Suppress("UnusedReceiverParameter")
@Composable
fun ColumnScope.SystemEntities(
    entities: List<SystemEntity>,
    system: String,
) {
    entities.filterIsInstance<SystemEntity.Killmail>().forEach { killmail ->
        ClickableEntity(
            onClick = {
                killmail.url.toURIOrNull()?.openBrowser()
            },
        ) {
            val text = buildString {
                append("Kill")
                if (killmail.ship != null) {
                    append(": ")
                    append(killmail.ship)
                }
            }
            IconInfoRow(
                icon = Res.drawable.keywords_killreport,
                text = text,
            )
        }
    }
    entities.filterIsInstance<SystemEntity.Ship>().forEach { ship ->
        SystemEntityInfoRow {
            val repository: ShipTypesRepository by koin.inject()
            val shipTypeId = repository.getShipTypeId(ship.name)
            AsyncTypeIcon(
                typeId = shipTypeId,
                modifier = Modifier.size(32.dp),
            )

            var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
            if (ship.isFriendly == true) {
                nameStyle = nameStyle.copy(color = RiftTheme.colors.standingBlue)
            }

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
    entities.filterIsInstance<SystemEntity.Character>()
        .sortedWith(compareBy({ it.details.allianceId }, { it.details.corporationId }))
        .forEach { character ->
            ClickablePlayer(character.characterId) {
                SystemEntityInfoRow {
                    AsyncPlayerPortrait(
                        characterId = character.characterId,
                        size = 32,
                        modifier = Modifier.size(32.dp),
                    )
                    RiftTooltipArea(
                        tooltip = character.details.corporationName ?: "",
                        anchor = TooltipAnchor.BottomCenter,
                    ) {
                        AsyncCorporationLogo(
                            corporationId = character.details.corporationId,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    if (character.details.allianceId != null) {
                        RiftTooltipArea(
                            tooltip = character.details.allianceName ?: "",
                            anchor = TooltipAnchor.BottomCenter,
                        ) {
                            AsyncAllianceLogo(
                                allianceId = character.details.allianceId,
                                size = 32,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
                        if (character.details.isFriendly) {
                            nameStyle = nameStyle.copy(color = RiftTheme.colors.standingBlue)
                        }
                        Text(
                            text = character.name,
                            style = nameStyle,
                        )
                        Text(
                            text = buildString {
                                character.details.corporationTicker?.let { append("$it ") }
                                character.details.allianceTicker?.let { append(it) }
                            },
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
            }
        }
    entities.firstOrNull { it is SystemEntity.UnspecifiedCharacter }?.let {
        val hasNamedHostiles =
            (entities.filterIsInstance<SystemEntity.Character>() + entities.filterIsInstance<SystemEntity.Ship>()).isNotEmpty()
        val unspecified = it as SystemEntity.UnspecifiedCharacter
        Text(
            text = "${if (hasNamedHostiles) "+" else ""}${unspecified.count} hostiles",
            style = RiftTheme.typography.bodyHighlighted,
            modifier = Modifier.padding(4.dp),
        )
    }
    entities.forEach { entity ->
        when (entity) {
            SystemEntity.Bubbles -> IconInfoRow(Res.drawable.keywords_interdiction_probe, "Bubbles")
            SystemEntity.CombatProbes -> IconInfoRow(Res.drawable.keywords_combat_probe, "Combat probes")
            SystemEntity.Ess -> IconInfoRow(Res.drawable.keywords_ess, "ESS")
            is SystemEntity.Gate -> GateInfoRow(system, entity)
            SystemEntity.GateCamp -> IconInfoRow(Res.drawable.keywords_gatecamp, "Gate camp")
            SystemEntity.NoVisual -> NoVisualRow()
            SystemEntity.Spike -> IconInfoRow(Res.drawable.keywords_spike, "Spike")
            SystemEntity.Wormhole -> WormholeInfoRow()
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
fun SystemEntityInfoRow(
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = 32.dp),
        content = content,
    )
}

@Composable
private fun IconInfoRow(icon: DrawableResource, text: String) {
    SystemEntityInfoRow {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = text,
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun WormholeInfoRow() {
    SystemEntityInfoRow {
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
                .size(32.dp)
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
private fun GateInfoRow(system: String, entity: SystemEntity.Gate) {
    SystemEntityInfoRow {
        GateIcon(entity.isAnsiblex, system, entity.system, 32.dp)
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(32.dp))
        val gateText = if (entity.isAnsiblex) "Ansiblex" else "Gate"
        Text(
            text = "${entity.system} $gateText",
            style = RiftTheme.typography.bodyHighlighted,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun NoVisualRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = 16.dp),
    ) {
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
}
