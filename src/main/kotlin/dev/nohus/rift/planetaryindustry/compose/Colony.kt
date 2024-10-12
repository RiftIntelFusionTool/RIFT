package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.SolarSystemPill
import dev.nohus.rift.compose.SolarSystemPillState
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.clock_16
import dev.nohus.rift.generated.resources.fastforward
import dev.nohus.rift.generated.resources.pi_disc_shadow
import dev.nohus.rift.generated.resources.pi_ecu_bottom
import dev.nohus.rift.generated.resources.pi_ecu_top
import dev.nohus.rift.generated.resources.pi_needsattentionicon
import dev.nohus.rift.generated.resources.pi_processor
import dev.nohus.rift.generated.resources.planet_barren_128
import dev.nohus.rift.generated.resources.planet_gas_128
import dev.nohus.rift.generated.resources.planet_ice_128
import dev.nohus.rift.generated.resources.planet_lava_128
import dev.nohus.rift.generated.resources.planet_ocean_128
import dev.nohus.rift.generated.resources.planet_plasma_128
import dev.nohus.rift.generated.resources.planet_storm_128
import dev.nohus.rift.generated.resources.planet_temperate_128
import dev.nohus.rift.network.esi.PlanetType
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.ColonyStatus
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Extracting
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Idle
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NeedsAttention
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NotSetup
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Producing
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorExpired
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorInactive
import dev.nohus.rift.planetaryindustry.models.PinStatus.StorageFull
import dev.nohus.rift.planetaryindustry.models.getName
import dev.nohus.rift.utils.formatDateTime
import dev.nohus.rift.utils.formatDurationCompact
import dev.nohus.rift.utils.invertedPlural
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.roundSecurity
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant

/**
 * Colony title row
 */
@Composable
fun ColonyTitle(
    item: ColonyItem,
    isExpanded: Boolean,
    isViewingFastForward: Boolean,
    onViewFastForwardChange: (Boolean) -> Unit,
    scrollState: ScrollState? = null,
    colonyIconModifier: Modifier = Modifier,
    onDetailsClick: () -> Unit,
) {
    val colony = item.colony
    val borderColor = RiftTheme.colors.borderPrimaryDark
    val borderAlpha by animateFloatAsState(if (scrollState?.canScrollBackward == true) 1f else 0f)
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color.Transparent, RiftTheme.colors.backgroundPrimaryDark))),
        ) {
            ColonyIcon(colony, colonyIconModifier)
            Column(
                modifier = Modifier.padding(vertical = Spacing.small),
            ) {
                ColonyOwner(colony, item.characterName)
                SolarSystemPill(
                    SolarSystemPillState(
                        distance = item.distance,
                        name = colony.planet.name,
                        security = colony.system.security.roundSecurity(),
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
            ExpiresIn(item, isViewingFastForward, onViewFastForwardChange)
            RiftButton(
                text = if (isExpanded) "Return" else "Details",
                onClick = onDetailsClick,
            )
        }
        AnimatedVisibility(isViewingFastForward) {
            ViewingFastForward(item, onReturnClick = { onViewFastForwardChange(false) })
        }
        AnimatedVisibility(scrollState != null) {
            Box(
                Modifier
                    .modifyIf(isViewingFastForward) { padding(top = Spacing.medium) }
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(borderColor.copy(alpha = borderAlpha)),
            )
        }
    }
}

@Composable
private fun ViewingFastForward(
    item: ColonyItem,
    onReturnClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = Spacing.medium),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = formatDateTime(item.ffwdColony.currentSimTime),
                style = RiftTheme.typography.titlePrimary,
            )
            getFutureColonyStatusDescription(item.ffwdColony.status)?.let {
                Text(
                    text = it,
                    style = RiftTheme.typography.bodyPrimary,
                )
            }
        }
        RiftButton(
            text = "Present time",
            icon = Res.drawable.clock_16,
            type = ButtonType.Secondary,
            isCompact = false,
            onClick = onReturnClick,
        )
    }
}

private fun getFutureColonyStatusDescription(status: ColonyStatus): String? {
    val state = when (status) {
        is NeedsAttention -> {
            val expired = status.pins.count { it.status == ExtractorExpired }
            val inactive = status.pins.count { it.status == ExtractorInactive }
            val full = status.pins.filter { it.status == StorageFull }
            buildList {
                if (expired > 0) add("Extractor${expired.plural} expire${inactive.invertedPlural}")
                if (inactive > 0) add("Extractor${inactive.plural} become${inactive.invertedPlural} inactive")
                full.forEach { add("${it.getName()} becomes full") }
            }.joinToString()
        }
        is Idle -> "All production stops"
        is NotSetup, is Producing, is Extracting -> return null // Should never happen
    }
    return state
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpiresIn(
    item: ColonyItem,
    isViewingFastForward: Boolean,
    onViewFastForwardChange: (Boolean) -> Unit,
) {
    val expiresIn = Duration.between(getNow(), item.ffwdColony.currentSimTime)
    if (expiresIn.toSeconds() > 0) {
        RiftTooltipArea(
            tooltip = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(Spacing.large),
                ) {
                    Image(
                        painter = painterResource(if (isViewingFastForward) Res.drawable.clock_16 else Res.drawable.fastforward),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (isViewingFastForward) "Return to present" else "Fast-forward",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            },
        ) {
            val pointerInteraction = rememberPointerInteractionStateHolder()
            val borderAlpha by animateFloatAsState(if (pointerInteraction.isHovered) 1f else 0f)
            Box(
                modifier = Modifier
                    .pointerInteraction(pointerInteraction)
                    .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                    .background(RiftTheme.colors.backgroundHovered.copy(alpha = borderAlpha))
                    .border(1.dp, RiftTheme.colors.borderGreyLight.copy(alpha = borderAlpha))
                    .padding(Spacing.small)
                    .onClick { onViewFastForwardChange(!isViewingFastForward) },
            ) {
                TitledText(
                    title = "Expires in",
                    text = formatDurationCompact(expiresIn),
                )
            }
        }
    }
}

/**
 * List of pin spheres
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColonyOverview(
    colony: Colony,
    now: Instant,
    isAdvancingTime: Boolean,
    onRequestSimulation: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.medium),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            colony.pins.sort().forEach { pin ->
                key(pin.id) {
                    PinSphere(colony, pin, 64.dp, now, isAdvancingTime, onRequestSimulation)
                }
            }
        }
    }
}

/**
 * Grid view planet icon
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColonyPlanetSnippet(
    item: ColonyItem,
    isShowingCharacter: Boolean,
    colonyIconModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    onExpandClick: () -> Unit,
) {
    val colony = item.colony
    val pointerInteraction = rememberPointerInteractionStateHolder()
    Column(
        modifier = modifier
            .pointerInteraction(pointerInteraction)
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .onClick { onExpandClick() },
    ) {
        Box(
            modifier = Modifier.modifyIf(isShowingCharacter) { Modifier.size(80.dp) },
        ) {
            Box {
                val hoverAlpha by animateFloatAsState(if (pointerInteraction.isHovered) 1f else 0f)
                Image(
                    painter = painterResource(Res.drawable.pi_disc_shadow),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .size(64.dp)
                        .scale(1.2f)
                        .alpha(hoverAlpha)
                        .align(Alignment.TopStart),
                )
                ColonyIcon(
                    colony = colony,
                    modifier = colonyIconModifier
                        .size(64.dp)
                        .align(Alignment.TopStart),
                )
            }
            if (isShowingCharacter) {
                val animatable = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(200)
                    animatable.animateTo(1f, tween(500))
                }
                Box(
                    modifier = Modifier
                        .scale(animatable.value)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
                ) {
                    AsyncPlayerPortrait(
                        characterId = colony.characterId,
                        size = 32,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

/**
 * List of pins with details
 */
@Composable
fun ColonyPins(
    colony: Colony,
    now: Instant,
    isAdvancingTime: Boolean,
    onRequestSimulation: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        colony.pins.sort().forEach { pin ->
            Pin(pin, colony, now, isAdvancingTime, onRequestSimulation)
        }
    }
}

@Composable
private fun ColonyOwner(
    colony: Colony,
    characterName: String?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 200.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
        ) {
            AsyncPlayerPortrait(
                characterId = colony.characterId,
                size = 32,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = characterName ?: "Loading…",
            style = RiftTheme.typography.titlePrimary,
        )
    }
}

private fun List<Pin>.sort(): List<Pin> {
    val order = listOf(
        Pin.CommandCenter::class,
        Pin.Launchpad::class,
        Pin.Storage::class,
        Pin.Extractor::class,
        Pin.Factory::class,
    )
    return sortedWith(
        compareBy(
            {
                order.indexOf(it::class)
            },
            {
                (it as? Pin.Factory)?.schematic?.outputType?.id ?: 0
            },
            {
                it.designator
            },
        ),
    )
}

@Composable
fun ColonyIcon(
    colony: Colony,
    modifier: Modifier = Modifier,
) {
    val type = colony.type
    RiftTooltipArea(
        tooltip = {
            Column(
                modifier = Modifier.padding(Spacing.large),
            ) {
                when (colony.status) {
                    is Extracting -> Text("Extracting", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textGreen)
                    is Producing -> Text("Producing", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textGreen)
                    is NotSetup -> Text("Not setup", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textRed)
                    is NeedsAttention -> Text("Needs attention", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textRed)
                    is Idle -> Text("Idle", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = colony.planet.name,
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = "${type.name} planet",
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        },
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            val icon = when (type) {
                PlanetType.Temperate -> Res.drawable.planet_temperate_128
                PlanetType.Barren -> Res.drawable.planet_barren_128
                PlanetType.Oceanic -> Res.drawable.planet_ocean_128
                PlanetType.Ice -> Res.drawable.planet_ice_128
                PlanetType.Gas -> Res.drawable.planet_gas_128
                PlanetType.Lava -> Res.drawable.planet_lava_128
                PlanetType.Storm -> Res.drawable.planet_storm_128
                PlanetType.Plasma -> Res.drawable.planet_plasma_128
            }
            val colorFilter = if (colony.status is NeedsAttention) {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.25f) })
            } else {
                null
            }
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = colorFilter,
                modifier = Modifier.size(64.dp),
            )

            val transition = rememberInfiniteTransition()
            when (colony.status) {
                is Extracting -> {
                    Image(
                        painter = painterResource(Res.drawable.pi_disc_shadow),
                        contentDescription = null,
                        alpha = 0.5f,
                        modifier = Modifier.size(32.dp),
                    )
                    Image(
                        painter = painterResource(Res.drawable.pi_ecu_bottom),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                    val shift by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    )
                    val maxShift = LocalDensity.current.run { -8.dp.toPx() }
                    Image(
                        painter = painterResource(Res.drawable.pi_ecu_top),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).graphicsLayer(translationY = maxShift * shift),
                    )
                }
                is Producing -> {
                    Image(
                        painter = painterResource(Res.drawable.pi_disc_shadow),
                        contentDescription = null,
                        alpha = 0.5f,
                        modifier = Modifier.size(32.dp),
                    )
                    val rotation by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing)),
                    )
                    Image(
                        painter = painterResource(Res.drawable.pi_processor),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).rotate(rotation),
                    )
                }
                is NotSetup, is NeedsAttention -> {
                    val alpha by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Reverse),
                    )
                    Image(
                        painter = painterResource(Res.drawable.pi_needsattentionicon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFFFF0000)),
                        alpha = alpha,
                        modifier = Modifier.size(31.dp),
                    )
                }
                is Idle -> {}
            }
        }
    }
}
