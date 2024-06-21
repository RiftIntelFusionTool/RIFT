package dev.nohus.rift.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.times
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.brackets_entity
import dev.nohus.rift.generated.resources.brackets_fightersquad_16
import dev.nohus.rift.intel.state.IntelStateController.Dated
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase.OnlineCharacterLocation
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class NodeSizes(
    val margin: Dp,
    val marginPx: Float,
    val radius: Dp,
    val radiusPx: Float,
) {
    val radiusWithMargin = margin + radius
    val radiusWithMarginPx = marginPx + radiusPx
}

const val SOLAR_SYSTEM_NODE_BACKGROUND_CIRCLE_MAX_SCALE = 0.61f

@Composable
fun SolarSystemNode(
    system: MapSolarSystem,
    mapType: MapType,
    mapScale: Float,
    intel: List<Dated<SystemEntity>>?,
    onlineCharacters: List<OnlineCharacterLocation>,
    systemColorStrategy: SystemColorStrategy,
    systemRadialGradients: MutableMap<Color, Brush>,
    nodeSizes: NodeSizes,
    modifier: Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        val systemColor = systemColorStrategy.getActiveColor(system.id)
        val brush = systemRadialGradients.getOrPut(systemColor) {
            Brush.radialGradient(
                0.0f to systemColor.copy(alpha = 1.0f),
                0.5f to systemColor.copy(alpha = 0.9f),
                0.7f to systemColor.copy(alpha = 0.7f),
                1.0f to systemColor.copy(alpha = 0f),
                radius = nodeSizes.radiusPx,
                center = Offset.Zero,
            )
        }
        val mapBackground = RiftTheme.colors.mapBackground

        val hostileOrbitPainter = remember { HostileOrbitPainter() }
        val characterLocationPainter = remember { CharacterLocationPainter() }
        hostileOrbitPainter.updateComposition()
        val hasOnlineCharacter = onlineCharacters.any { it.location.solarSystemId == system.id }

        val hostileCount = hostileOrbitPainter.getHostileCount(intel)
        val entityIcons = hostileOrbitPainter.getEntityIcons(intel)
        val nodeBackgroundCircleMaxScale = SOLAR_SYSTEM_NODE_BACKGROUND_CIRCLE_MAX_SCALE / LocalDensity.current.density

        Canvas(
            modifier = Modifier.size(2 * nodeSizes.radiusWithMargin),
        ) {
            if (mapType is RegionMap && mapScale <= nodeBackgroundCircleMaxScale) {
                drawCircle(mapBackground, radius = nodeSizes.radiusWithMarginPx, center = Offset.Zero)
            }
            drawCircle(brush, radius = nodeSizes.radiusPx, center = Offset.Zero)
            hostileOrbitPainter.draw(this, nodeSizes, hostileCount, entityIcons)
            characterLocationPainter.draw(this, nodeSizes, hasOnlineCharacter)
        }
    }
}

class CharacterLocationPainter {

    fun draw(
        scope: DrawScope,
        nodeSizes: NodeSizes,
        hasOnlineCharacter: Boolean,
    ) = with(scope) {
        if (!hasOnlineCharacter) return@with

        val startRadius = nodeSizes.radiusPx - 2 * density
        val endRadius = nodeSizes.radiusPx + 2 * density
        val centerRadius = nodeSizes.radiusPx
        val color = Color.Green
        val brush = Brush.radialGradient(
            0.0f to color.copy(alpha = 0.0f),
            (startRadius / endRadius) to color.copy(alpha = 0.0f),
            (centerRadius / endRadius) to color.copy(1.0f),
            1.0f to color.copy(alpha = 0.0f),
            radius = endRadius,
            center = Offset.Zero,
        )
        drawCircle(brush, radius = endRadius, center = Offset.Zero, style = Fill)
    }
}

class HostileOrbitPainter {

    private val shipTypesRepository: ShipTypesRepository by koin.inject()
    private val bitmapsCache = mutableMapOf<DrawableResource, ImageBitmap>()
    private val bitmapOffset = Offset(8f, 8f)
    private var animationRotation by mutableStateOf(0f)
    private val orbitBrushForColor = mutableMapOf<Color, Brush>()
    private val friendlyEntityRadius = 10f
    private lateinit var friendlyEntityBrush: Brush

    data class EntityIcon(
        val bitmap: ImageBitmap,
        val isFriendly: Boolean,
    )

    @Composable
    fun updateComposition() {
        val transition = rememberInfiniteTransition()
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(tween(5_000, easing = LinearEasing)),
        )
        this.animationRotation = rotation
        this.friendlyEntityBrush = Brush.radialGradient(
            0.0f to RiftTheme.colors.standingBlue.copy(alpha = 1.0f),
            1.0f to RiftTheme.colors.standingBlue.copy(alpha = 0f),
            radius = friendlyEntityRadius,
            center = Offset.Zero,
        )
    }

    fun draw(
        scope: DrawScope,
        nodeSizes: NodeSizes,
        hostileCount: Int,
        entityIcons: List<EntityIcon>,
    ) = with(scope) {
        if (hostileCount > 0) {
            val endRadius = nodeSizes.radiusWithMarginPx
            val startRadius = nodeSizes.radiusPx
            val centerRadius = (startRadius + endRadius) / 2
            val color = getColor(hostileCount / 5f)
            val brush = orbitBrushForColor.getOrPut(color) {
                Brush.radialGradient(
                    0.0f to color.copy(alpha = 0.0f),
                    (startRadius / endRadius) to color.copy(alpha = 0.0f),
                    (centerRadius / endRadius) to color.copy(1.0f),
                    1.0f to color.copy(alpha = 0.0f),
                    radius = endRadius,
                    center = Offset.Zero,
                )
            }
            drawCircle(brush, radius = endRadius, center = Offset.Zero, style = Fill)
        }

        if (entityIcons.isEmpty()) return@with
        entityIcons.forEachIndexed { index, icon ->
            val offsetAngle = 360 / entityIcons.size
            val angle = Math.toRadians(animationRotation.toDouble() + (index * offsetAngle)).toFloat()
            val radius = nodeSizes.radiusPx + (nodeSizes.marginPx / 2)
            val center = Offset(radius * sin(angle), radius * cos(angle))

            translate(left = center.x.roundToInt().toFloat(), top = center.y.roundToInt().toFloat()) {
                if (icon.isFriendly) drawCircle(friendlyEntityBrush, radius = friendlyEntityRadius, center = Offset(-1f, -1f))
                translate(left = -bitmapOffset.x, top = -bitmapOffset.y) {
                    drawImage(
                        image = icon.bitmap,
                        dstSize = IntSize(icon.bitmap.width, icon.bitmap.height) * scope.density.toInt(),
                        dstOffset = IntOffset(icon.bitmap.width * (1 - scope.density.toInt()) / 2, icon.bitmap.height * (1 - scope.density.toInt()) / 2),
                    )
                }
            }
        }
    }

    fun getHostileCount(intel: List<Dated<SystemEntity>>?): Int {
        if (intel == null) return 0
        val entities = intel.map { it.item }
        val characterCount = entities.filterIsInstance<SystemEntity.Character>().count() + entities.filterIsInstance<SystemEntity.UnspecifiedCharacter>().sumOf { it.count }
        val shipCount = entities.filterIsInstance<SystemEntity.Ship>().sumOf { it.count }
        var count = maxOf(characterCount, shipCount)
        if (entities.any { it is SystemEntity.GateCamp }) count += 3
        if (entities.any { it is SystemEntity.Spike }) count += 10
        return count
    }

    private fun getColor(percentage: Float): Color {
        val safePercentage = percentage.coerceIn(0f, 1f)
        val lowColor = Color(0xFF956308)
        val highColor = Color(0xFFC30304)
        return lerp(lowColor, highColor, safePercentage)
    }

    @Composable
    fun getEntityIcons(intel: List<Dated<SystemEntity>>?): List<EntityIcon> {
        val hostileCount = getHostileCount(intel)
        val hostileIcons = getHostileIcons(hostileCount, intel)
        val friendlyIcons = getFriendlyIcons(intel)
        val maxIcons = 10
        val hostileMaxIcons = if (hostileIcons.size >= friendlyIcons.size * 2) {
            (10 - friendlyIcons.size).coerceAtLeast(7)
        } else if (friendlyIcons.size >= hostileIcons.size * 2) {
            (0 + friendlyIcons.size).coerceAtMost(3)
        } else {
            5
        }
        val friendlyMaxIcons = maxIcons - hostileMaxIcons
        return getGroupedImageBitmaps(hostileIcons, hostileMaxIcons).map { EntityIcon(it, isFriendly = false) } +
            getGroupedImageBitmaps(friendlyIcons, friendlyMaxIcons).map { EntityIcon(it, isFriendly = true) }
    }

    @Composable
    private fun getHostileIcons(
        characterCount: Int,
        intel: List<Dated<SystemEntity>>?,
    ): List<DrawableResource> {
        if (intel == null) return emptyList()
        val specificIcons = getIcons(intel.filter { it.item is SystemEntity.Ship && it.item.isFriendly != true })
        val unknownCharactersCount = (characterCount - specificIcons.size).coerceAtLeast(0)
        val friendlyCharacterCount = intel.count { it.item is SystemEntity.Ship && it.item.isFriendly == true }
        val hostileCharacterCount = unknownCharactersCount - friendlyCharacterCount
        val hostileCharactersIcons = List(hostileCharacterCount) { Res.drawable.brackets_entity }
        return specificIcons + hostileCharactersIcons
    }

    @Composable
    private fun getFriendlyIcons(
        intel: List<Dated<SystemEntity>>?,
    ): List<DrawableResource> {
        if (intel == null) return emptyList()
        val specificIcons = getIcons(intel.filter { it.item is SystemEntity.Ship && it.item.isFriendly == true })
        return specificIcons
    }

    @Composable
    private fun getGroupedImageBitmaps(drawables: List<DrawableResource>, maxIcons: Int): List<ImageBitmap> {
        var icons = drawables
        if (icons.size > maxIcons) {
            icons = List((icons.size / 3).coerceAtMost(maxIcons)) { Res.drawable.brackets_fightersquad_16 }
        }
        return icons.map {
            bitmapsCache.getOrPut(it) { imageResource(it) }
        }
    }

    private fun getIcons(items: List<Dated<SystemEntity>>): List<DrawableResource> {
        return items.mapNotNull {
            val ship = it.item as SystemEntity.Ship
            val icon = shipTypesRepository.getShipBracketIcon(ship.name) ?: return@mapNotNull null
            List(it.item.count) { icon }
        }.flatten()
    }
}
