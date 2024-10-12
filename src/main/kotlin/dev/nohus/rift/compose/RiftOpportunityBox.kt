package dev.nohus.rift.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.careerpaths_enforcer_16px
import dev.nohus.rift.generated.resources.careerpaths_enforcer_flair
import dev.nohus.rift.generated.resources.careerpaths_explorer_16px
import dev.nohus.rift.generated.resources.careerpaths_explorer_flair
import dev.nohus.rift.generated.resources.careerpaths_industrialist_16px
import dev.nohus.rift.generated.resources.careerpaths_industrialist_flair
import dev.nohus.rift.generated.resources.careerpaths_sof_flair
import dev.nohus.rift.generated.resources.careerpaths_soldier_of_fortune_16px
import dev.nohus.rift.generated.resources.careerpaths_unclassified_16px
import dev.nohus.rift.generated.resources.careerpaths_unclassified_flair
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.utils.roundSecurity
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import java.time.Instant
import java.time.ZoneId

sealed class RiftOpportunityBoxCategory(
    val icon: DrawableResource,
    val flair: DrawableResource,
) {
    data object Enforcer : RiftOpportunityBoxCategory(
        icon = Res.drawable.careerpaths_enforcer_16px,
        flair = Res.drawable.careerpaths_enforcer_flair,
    )
    data object SoldierOfFortune : RiftOpportunityBoxCategory(
        icon = Res.drawable.careerpaths_soldier_of_fortune_16px,
        flair = Res.drawable.careerpaths_sof_flair,
    )
    data object Industrialist : RiftOpportunityBoxCategory(
        icon = Res.drawable.careerpaths_industrialist_16px,
        flair = Res.drawable.careerpaths_industrialist_flair,
    )
    data object Explorer : RiftOpportunityBoxCategory(
        icon = Res.drawable.careerpaths_explorer_16px,
        flair = Res.drawable.careerpaths_explorer_flair,
    )
    data object Unclassified : RiftOpportunityBoxCategory(
        icon = Res.drawable.careerpaths_unclassified_16px,
        flair = Res.drawable.careerpaths_unclassified_flair,
    )
}

data class SolarSystemPillState(
    val distance: Int?,
    val name: String,
    val security: Double?,
)

data class RiftOpportunityBoxCharacter(
    val name: String,
    val id: Int?,
)

data class RiftOpportunityBoxButton(
    val resource: DrawableResource,
    val tooltip: String,
    val action: () -> Unit,
)

@Composable
fun RiftOpportunityBox(
    category: RiftOpportunityBoxCategory,
    type: AnnotatedString,
    locations: List<SolarSystemPillState>,
    character: RiftOpportunityBoxCharacter?,
    title: String?,
    timestamp: Instant,
    displayTimezone: ZoneId,
    buttons: List<RiftOpportunityBoxButton>,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Row(
        modifier = Modifier
            .pointerInteraction(pointerInteractionStateHolder)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .width(7.dp)
                .offset(4.dp)
                .zIndex(1f),
        ) {
            val alpha by animateFloatAsState(if (pointerInteractionStateHolder.isHovered) 0.5f else 0.1f)
            val blur by animateFloatAsState(if (pointerInteractionStateHolder.isHovered) 4f else 0.5f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                    .background(RiftTheme.colors.primary.copy(alpha = alpha)),
            ) {}
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(RiftTheme.colors.primary.copy(alpha = 0.5f)),
            ) {}
        }
        val background by animateColorAsState(if (pointerInteractionStateHolder.isHovered) RiftTheme.colors.backgroundPrimary else RiftTheme.colors.backgroundPrimaryDark)
        Surface(
            color = background,
            shape = CutCornerShape(bottomEnd = 15.dp),
        ) {
            Box {
                OpportunityContainerFlair(
                    isHovered = pointerInteractionStateHolder.isHovered,
                    image = category.flair,
                )
                Column {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = Spacing.large, vertical = Spacing.mediumLarge),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = Spacing.medium),
                                ) {
                                    Surface(
                                        color = RiftTheme.colors.backgroundPrimaryLight,
                                        shape = CircleShape,
                                        modifier = Modifier.alpha(0.5f),
                                    ) {
                                        Image(
                                            painter = painterResource(category.icon),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .size(16.dp),
                                        )
                                    }
                                    Text(
                                        text = type,
                                        style = RiftTheme.typography.titleSecondary,
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                ) {
                                    for (location in locations) {
                                        SolarSystemPill(location)
                                    }
                                }
                            }
                            if (character?.id != null) {
                                CharacterPortrait(character)
                            }
                        }
                        content()
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f)),
                    ) {
                        val now = getNow()
                        val age = key(now) { getRelativeTime(timestamp, displayTimezone) }
                        val text = buildAnnotatedString {
                            if (title != null) {
                                withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                                    append(title)
                                }
                                append(", ")
                            }
                            append(age)
                        }
                        Text(
                            text = text,
                            style = RiftTheme.typography.titleSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                                .padding(start = Spacing.large, end = Spacing.medium)
                                .padding(vertical = Spacing.mediumLarge),
                        )
                        if (buttons.isNotEmpty()) {
                            buttons.forEach { button ->
                                RiftTooltipArea(
                                    text = button.tooltip,
                                ) {
                                    RiftImageButton(
                                        resource = button.resource,
                                        size = 16.dp,
                                        iconPadding = 8.dp,
                                        onClick = button.action,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(Spacing.medium))
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterPortrait(character: RiftOpportunityBoxCharacter) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f))
            .padding(4.dp),
    ) {
        RiftTooltipArea(
            text = character.name,
        ) {
            ClickablePlayer(character.id) {
                AsyncPlayerPortrait(
                    characterId = character.id,
                    size = 64,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                )
            }
        }
    }
}

@Composable
fun SolarSystemPill(state: SolarSystemPillState) {
    ClickableSystem(state.name) {
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
            ) {
                if (state.distance != null) {
                    val text = when (state.distance) {
                        0 -> "Current System"
                        1 -> "1 jump"
                        in 2..9 -> "${state.distance} jumps"
                        else -> "10+ jumps"
                    }
                    Text(
                        text = text,
                        style = RiftTheme.typography.bodySecondary,
                        modifier = Modifier.padding(start = Spacing.small, end = Spacing.medium),
                    )
                }
                val securityColor = SecurityColors[state.security?.roundSecurity() ?: 0.0]
                Surface(
                    color = securityColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(2.dp),
                ) {
                    Text(
                        text = state.name,
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 1.dp),
                    )
                }
                if (state.security != null) {
                    Surface(
                        color = securityColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(2.dp),
                    ) {
                        Text(
                            text = state.security.roundSecurity().toString(),
                            style = RiftTheme.typography.bodyPrimary,
                            modifier = Modifier.padding(horizontal = Spacing.small, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpportunityContainerFlair(
    isHovered: Boolean,
    image: DrawableResource,
) {
    val animation = remember { Animatable(0f) }
    val target = -360f
    if (isHovered) {
        LaunchedEffect(Unit) {
            while (isActive) {
                val remaining = (target - animation.value) / target
                animation.animateTo(target, animationSpec = tween((80_000 * remaining).toInt(), easing = LinearEasing))
                animation.snapTo(0f)
            }
        }
    }

    val bitmap = imageResource(image)
    val size = 500
    val color = RiftTheme.colors.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        translate(Spacing.large.toPx(), Spacing.mediumLarge.toPx()) {
            rotate(animation.value, pivot = Offset.Zero) {
                translate(-size / 2f, -size / 2f) {
                    drawImage(
                        image = bitmap,
                        dstSize = IntSize(size, size),
                        alpha = 0.35f,
                        colorFilter = ColorFilter.tint(color),
                    )
                }
            }
        }
    }
}
