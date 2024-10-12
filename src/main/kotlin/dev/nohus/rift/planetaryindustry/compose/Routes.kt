package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.Commodities
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.Route
import dev.nohus.rift.planetaryindustry.models.getIcon
import dev.nohus.rift.planetaryindustry.models.getName
import org.jetbrains.compose.resources.painterResource

@Composable
fun Routes(
    colony: Colony,
    pin: Pin,
) {
    Column(
        modifier = Modifier.padding(top = Spacing.medium),
    ) {
        Text(
            text = "Routes",
            style = RiftTheme.typography.bodyHighlighted,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
        val routes = colony.routes.sortedWith(compareBy({ it.type.id }, { it.sourcePinId }, { it.destinationPinId }))
        val routesFrom = routes.filter { it.sourcePinId == pin.id }
        val routesTo = routes.filter { it.destinationPinId == pin.id }
        routesFrom.forEach { route ->
            Route(route, colony, true)
        }
        routesTo.forEach { route ->
            Route(route, colony, false)
        }
    }
}

@Composable
private fun Route(
    route: Route,
    colony: Colony,
    isOutgoing: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiftTooltipArea(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                    append(route.type.name)
                }
                append(" ")
                withStyle(SpanStyle(color = RiftTheme.colors.textSecondary)) {
                    append(Commodities.getTierName(route.type.name))
                }
            },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                Text(
                    text = "${route.quantity.toInt()} x",
                    style = RiftTheme.typography.bodyPrimary,
                )
                AsyncTypeIcon(
                    type = route.type,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = if (isOutgoing) " exporting to" else " importing from",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(end = Spacing.small),
        )
        colony.pins
            .firstOrNull {
                if (isOutgoing) it.id == route.destinationPinId else it.id == route.sourcePinId
            }?.let { other ->
                RiftTooltipArea(
                    text = other.getName(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(other.getIcon()),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = other.designator,
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                }
            }
    }
}
