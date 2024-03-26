package dev.nohus.rift.map

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.nohus.rift.characters.OnlineIndicatorDot
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase
import dev.nohus.rift.map.systemcolor.RegionColors
import dev.nohus.rift.repositories.SolarSystemsRepository.MapRegion

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun RegionNode(
    region: MapRegion,
    mapScale: Float,
    onlineCharacters: List<GetOnlineCharactersLocationUseCase.OnlineCharacterLocation>,
    isHighlightedOrHovered: Boolean,
    onPointerEnter: () -> Unit,
    onPointerExit: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        val textMeasurer = rememberTextMeasurer()
        val textStyle = if (isHighlightedOrHovered) RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold) else RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold)
        val measurement = textMeasurer.measure(region.name, style = textStyle)
        val textWidthDp = LocalDensity.current.run { measurement.size.width.toDp() }
        val textHeightDp = LocalDensity.current.run { measurement.size.height.toDp() }
        val padding = 8.dp
        val color = RegionColors.getColor(region.name)
        val roundedPercent by animateIntAsState(if (isHighlightedOrHovered) 25 else 100, animationSpec = tween(500))
        val shape = RoundedCornerShape(roundedPercent)
        val hasOnlineCharacters = onlineCharacters.any { it.location.regionId == region.id }
        Box(
            modifier = Modifier
                .offset(x = -(textWidthDp / 2 + padding), y = -(textHeightDp / 2 + padding)),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp) // Space for online indicators
                    .background(RiftTheme.colors.mapBackground, shape)
                    .background(color.copy(alpha = 0.2f), shape)
                    .border(if (isHighlightedOrHovered) 2.dp else 1.dp, color, shape)
                    .onPointerEvent(PointerEventType.Enter) {
                        onPointerEnter()
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        onPointerExit()
                    }
                    .onClick {
                        onClick()
                    }
                    .padding(padding),
            ) {
                Column {
                    Text(
                        text = region.name,
                        style = textStyle,
                    )
                }
            }
            if (hasOnlineCharacters) {
                OnlineIndicatorDot(
                    isOnline = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = 2.dp),
                )
            }
        }
    }
}
