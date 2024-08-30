package dev.nohus.rift.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.repositories.AbyssalSystemNames
import dev.nohus.rift.repositories.GetSystemDistanceFromCharacterUseCase
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.WormholeRegionClasses
import java.time.Duration
import java.time.Instant

@Composable
fun IntelSystem(
    system: String,
    rowHeight: Dp,
    isShowingSystemDistance: Boolean,
    isUsingJumpBridges: Boolean,
    background: Color = Color.Transparent,
) {
    val repository: SolarSystemsRepository by koin.inject()
    ClickableSystem(system) {
        BorderedToken(rowHeight, modifier = Modifier.background(background)) {
            val sunTypeId = repository.getSystemSunTypeId(system)
            AsyncTypeIcon(
                typeId = sunTypeId,
                modifier = Modifier.size(rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            Column(
                modifier = Modifier.padding(horizontal = Spacing.small),
            ) {
                val abyssalName = AbyssalSystemNames[system]
                if (abyssalName != null) {
                    Text(
                        text = abyssalName,
                        style = RiftTheme.typography.bodyTriglavian.copy(fontWeight = FontWeight.Bold, color = RiftTheme.colors.textLink),
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                } else {
                    Text(
                        text = system,
                        style = RiftTheme.typography.bodyLink.copy(fontWeight = FontWeight.Bold),
                    )
                    if (rowHeight >= 32.dp) {
                        repository.getRegionBySystem(system)?.let { region ->
                            val text = WormholeRegionClasses[region] ?: region
                            Text(
                                text = text,
                                style = RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }
                }
            }
            if (isShowingSystemDistance) {
                val systemId = repository.getSystemId(system) ?: return@BorderedToken
                SystemDistanceIndicator(systemId, rowHeight, isUsingJumpBridges)
            }
        }
    }
}

@Composable
private fun SystemDistanceIndicator(
    systemId: Int,
    height: Dp,
    isUsingJumpBridges: Boolean,
) {
    val getDistance: GetSystemDistanceFromCharacterUseCase by koin.inject()
    val distance = getDistance(systemId, maxDistance = 9, withJumpBridges = isUsingJumpBridges)
    if (distance > 9) return
    val distanceColor = getDistanceColor(distance)
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(height)
            .padding(top = 1.dp, bottom = 1.dp, end = 1.dp)
            .border(2.dp, distanceColor, RoundedCornerShape(100))
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = "$distance",
            style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier,
        )
    }
}

private fun getDistanceColor(distance: Int): Color {
    return when {
        distance >= 5 -> Color(0xFF2E74DF)
        distance >= 4 -> Color(0xFF4ACFF3)
        distance >= 3 -> Color(0xFF5CDCA6)
        distance >= 2 -> Color(0xFF70E552)
        distance >= 1 -> Color(0xFFDC6C08)
        else -> Color(0xFFBC1113)
    }
}

@Composable
fun IntelTimer(
    timestamp: Instant,
    style: TextStyle,
    rowHeight: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    val duration = Duration.between(timestamp, now)
    val colorFadePercentage = (duration.toSeconds() / Duration.ofMinutes(3).seconds.toFloat()).coerceIn(0f, 1f)
    val color = lerp(RiftTheme.colors.textSpecialHighlighted, RiftTheme.colors.textSecondary, colorFadePercentage)
    val borderColor = lerp(RiftTheme.colors.textSpecialHighlighted, RiftTheme.colors.borderGreyLight, colorFadePercentage)
    val content = @Composable {
        Text(
            text = formatDuration(duration),
            style = style.copy(color = color),
            modifier = modifier,
        )
    }
    if (rowHeight != null) {
        BorderedToken(rowHeight, borderColor) {
            content()
        }
    } else {
        content()
    }
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

@Composable
fun BorderedToken(
    rowHeight: Dp,
    borderColor: Color = RiftTheme.colors.borderGreyLight,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(IntrinsicSize.Max)
            .heightIn(min = rowHeight)
            .border(1.dp, borderColor),
        content = content,
    )
}
