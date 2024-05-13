package dev.nohus.rift.map.painter

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.map.DoubleOffset
import dev.nohus.rift.map.MapLayoutRepository
import dev.nohus.rift.map.MapLayoutRepository.Position
import dev.nohus.rift.map.MapViewModel.Cluster
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.systemcolor.SolarSystemColorStrategy
import dev.nohus.rift.repositories.MapGateConnectionsRepository
import dev.nohus.rift.repositories.MapGateConnectionsRepository.GateConnection
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

class SystemsMapPainter(
    private val cluster: Cluster,
    private val layout: Map<Int, Position>,
    private val jumpBridgeAdditionalSystems: Set<Int>,
    private val solarSystemColorStrategy: SolarSystemColorStrategy,
    private val mapType: MapType,
    private val isJumpBridgeNetworkShown: Boolean,
    private val jumpBridgeNetworkOpacity: Int,
    private val autopilotConnections: List<Pair<Int, Int>>,
) : MapPainter {

    private lateinit var textMeasurer: TextMeasurer
    private lateinit var regionNameStyle: TextStyle
    private val systemsIdsInLayout = layout.keys
    private val systemsInLayout = cluster.systems.filter {
        it.id in systemsIdsInLayout
    }
    private val systemsWithGateConnections = systemsIdsInLayout - jumpBridgeAdditionalSystems
    private val connectionsInLayout = cluster.connections.filter {
        it.from.id in systemsWithGateConnections && it.to.id in systemsWithGateConnections
    }
    data class JumpBridgeConnectionLine(val from: MapSolarSystem, val to: MapSolarSystem, val bidirectional: Boolean)
    private val jumpBridgeConnectionsInLayout = cluster.jumpBridgeConnections?.filter {
        it.from.id in systemsIdsInLayout || it.to.id in systemsIdsInLayout
    } ?: emptyList()
    private val jumpBridgeConnectionLines = jumpBridgeConnectionsInLayout.map { connection ->
        val (from, to) = listOf(connection.from, connection.to).sortedBy { it.id }
        val isBidirectional = jumpBridgeConnectionsInLayout.find { it.from == to && it.to == from } != null
        JumpBridgeConnectionLine(from, to, isBidirectional)
    }.distinct()
    private val systemRadialGradients = mutableMapOf<Color, Brush>()

    @Composable
    override fun initializeComposed() {
        textMeasurer = rememberTextMeasurer()
        regionNameStyle = RiftTheme.typography.captionPrimary.copy(letterSpacing = 3.sp)
    }

    override fun draw(
        scope: DrawScope,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        animationPercentage: Float,
    ) = with(scope) {
        connectionsInLayout.forEach { connection ->
            drawSystemConnection(connection, mapType, center, scale, zoom, animationPercentage)
        }
        if (isJumpBridgeNetworkShown) {
            jumpBridgeConnectionLines.forEach { connection ->
                drawJumpBridgeConnection(connection, mapType, center, scale, zoom, animationPercentage)
            }
        }
        if (mapType is MapType.ClusterSystemsMap) {
            systemsInLayout.forEach { system ->
                drawSystem(system, center, scale, zoom)
            }
            cluster.regions.forEach { region ->
                drawRegion(region, center, scale)
            }
        }
    }

    private fun DrawScope.drawRegion(
        region: SolarSystemsRepository.MapRegion,
        center: DoubleOffset,
        scale: Float,
    ) {
        val position = MapLayoutRepository.transformNewEdenCoordinate(region.x, region.z)
        val offset = getCanvasCoordinates(position.x, position.y, center, scale)
        if (isOnCanvas(offset, 100)) {
            val textLayout = textMeasurer.measure(region.name.uppercase(), style = regionNameStyle, softWrap = false)
            val topLeft = Offset(offset.x - textLayout.size.width.toFloat() / 2, offset.y - textLayout.size.height.toFloat() / 2)
            drawText(textLayout, topLeft = topLeft)
        }
    }

    private fun DrawScope.drawSystem(
        system: MapSolarSystem,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
    ) {
        val position = MapLayoutRepository.transformNewEdenCoordinate(system.x, system.z)
        val offset = getCanvasCoordinates(position.x, position.y, center, scale)
        if (isOnCanvas(offset, 100)) {
            val systemColor = solarSystemColorStrategy.getActiveColor(system)
            val radius = 4f * density
            val brush = systemRadialGradients.getOrPut(systemColor) {
                Brush.radialGradient(
                    listOf(systemColor.copy(alpha = 0.7f), systemColor.copy(alpha = 0f)),
                    radius = radius,
                    center = Offset.Zero,
                )
            }
            translate(offset.x, offset.y) {
                drawCircle(brush, radius = radius, center = Offset.Zero)
                drawCircle(systemColor, radius = (0.2f * density * zoom / 2), center = Offset.Zero)
            }
        }
    }

    private fun DrawScope.drawSystemConnection(
        connection: GateConnection,
        mapType: MapType,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        animation: Float,
    ) {
        val fromLayoutPosition = layout[connection.from.id]!!
        val toLayoutPosition = layout[connection.to.id]!!
        val from = getCanvasCoordinates(fromLayoutPosition.x, fromLayoutPosition.y, center, scale)
        val to = getCanvasCoordinates(toLayoutPosition.x, toLayoutPosition.y, center, scale)
        val deltaOffset = to - from

        val autopilotPathEffect = getAutopilotPathEffect(connection.from.id, connection.to.id, animation, zoom)
        if (connection.type == MapGateConnectionsRepository.ConnectionType.Region || scale < 4) {
            translate(from.x, from.y) {
                val width = (1f / scale).coerceAtMost(2f) * density
                if (autopilotPathEffect != null) {
                    val fromColor = solarSystemColorStrategy.getActiveColor(connection.from)
                    val toColor = solarSystemColorStrategy.getActiveColor(connection.to)
                    drawLine(
                        brush = Brush.linearGradient(listOf(fromColor.copy(alpha = 0.25f), toColor.copy(alpha = 0.25f)), start = Offset.Zero, end = deltaOffset),
                        start = Offset.Zero,
                        end = deltaOffset,
                        strokeWidth = width * 2,
                    )
                    drawLine(
                        brush = Brush.linearGradient(listOf(fromColor, toColor), start = Offset.Zero, end = deltaOffset),
                        start = Offset.Zero,
                        end = deltaOffset,
                        strokeWidth = width * 2,
                        pathEffect = autopilotPathEffect,
                    )
                } else {
                    val brush = if (mapType is RegionMap || scale < 0.5) {
                        val fromColor = solarSystemColorStrategy.getActiveColor(connection.from)
                        val toColor = solarSystemColorStrategy.getActiveColor(connection.to)
                        Brush.linearGradient(listOf(fromColor, toColor), start = Offset.Zero, end = deltaOffset)
                    } else {
                        val fromColor = solarSystemColorStrategy.getInactiveColor(connection.from)
                        val toColor = solarSystemColorStrategy.getInactiveColor(connection.to)
                        Brush.linearGradient(listOf(fromColor, toColor), start = Offset.Zero, end = deltaOffset)
                    }
                    val effect = when (connection.type) {
                        MapGateConnectionsRepository.ConnectionType.System -> null
                        MapGateConnectionsRepository.ConnectionType.Constellation -> PathEffect.dashPathEffect(floatArrayOf(6.0f * zoom * density, 2.0f * zoom * density))
                        MapGateConnectionsRepository.ConnectionType.Region -> PathEffect.dashPathEffect(floatArrayOf(15.0f * zoom * density, 5.0f * zoom * density))
                    }
                    drawLine(
                        brush = brush,
                        start = Offset.Zero,
                        end = deltaOffset,
                        strokeWidth = width,
                        pathEffect = effect,
                    )
                }
            }
        }
    }

    private fun DrawScope.drawJumpBridgeConnection(
        connection: JumpBridgeConnectionLine,
        mapType: MapType,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        animation: Float,
    ) {
        val fromLayoutPosition = layout[connection.from.id] ?: return
        val toLayoutPosition = layout[connection.to.id] ?: return
        val from = getCanvasCoordinates(fromLayoutPosition.x, fromLayoutPosition.y, center, scale)
        val to = getCanvasCoordinates(toLayoutPosition.x, toLayoutPosition.y, center, scale)
        val distance = sqrt((from.x - to.x).toDouble().pow(2.0) + (from.y - to.y).toDouble().pow(2.0)).toFloat()
        val (p1, p2) = listOf(from, to).sortedWith(compareBy({ it.x }, { it.y }))
        val isReversed = from == p2

        val width = (1f / scale).coerceAtMost(2f) * density
        val (autopilotFrom, autopilotTo) = listOf(connection.from.id, connection.to.id).let { if (isReversed) it.reversed() else it }
        val autopilotPathEffect = getAutopilotPathEffect(autopilotFrom, autopilotTo, animation, zoom)
        if (autopilotPathEffect != null) {
            val fromColor = solarSystemColorStrategy.getActiveColor(connection.from)
            val toColor = solarSystemColorStrategy.getActiveColor(connection.to)
            val bridgeColor = Color(0xFF75D25A)
            val colors = listOf(fromColor, bridgeColor, toColor)
            val brush1 = Brush.linearGradient(
                colors = colors.map { it.copy(alpha = 0.25f) },
                start = if (isReversed) p2 else p1,
                end = if (isReversed) p1 else p2,
            )
            val brush2 = Brush.linearGradient(
                colors = colors,
                start = if (isReversed) p2 else p1,
                end = if (isReversed) p1 else p2,
            )
            drawArcBetweenTwoPoints(p1, p2, distance * 0.75f, brush1, Stroke(width * 2 * density))
            drawArcBetweenTwoPoints(p1, p2, distance * 0.75f, brush2, Stroke(width * 2 * density, pathEffect = autopilotPathEffect))
        } else {
            val alphaModifier = jumpBridgeNetworkOpacity / 100f
            val toColorFilter: Color.(isBidirectional: Boolean) -> Color = { if (it) this else this.copy(alpha = 0.1f) }
            val colors = if (mapType is RegionMap || scale < 0.5) {
                val fromColor = solarSystemColorStrategy.getActiveColor(connection.from)
                val toColor = solarSystemColorStrategy.getActiveColor(connection.to)
                val bridgeColor = Color(0xFF75D25A)
                listOf(fromColor, bridgeColor, bridgeColor.toColorFilter(connection.bidirectional), toColor.toColorFilter(connection.bidirectional))
            } else {
                val fromColor = solarSystemColorStrategy.getInactiveColor(connection.from)
                val toColor = solarSystemColorStrategy.getInactiveColor(connection.to)
                val bridgeColor = Color(0xFF75D25A).copy(alpha = 0.1f)
                listOf(fromColor, bridgeColor, bridgeColor.toColorFilter(connection.bidirectional), toColor.toColorFilter(connection.bidirectional))
            }.map { it.copy(alpha = it.alpha * alphaModifier) }
            val brush = Brush.linearGradient(
                colors = colors,
                start = if (isReversed) p2 else p1,
                end = if (isReversed) p1 else p2,
            )
            val style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f * zoom * density, 5f * zoom * density)))
            drawArcBetweenTwoPoints(p1, p2, distance * 0.75f, brush, style)
        }
    }

    private fun DrawScope.getAutopilotPathEffect(from: Int, to: Int, animation: Float, zoom: Float): PathEffect? {
        val factor = animation * 96f * density
        val phase = when {
            autopilotConnections.any { it.first == from && it.second == to } -> (1f - factor) * zoom
            autopilotConnections.any { it.first == to && it.second == from } -> factor * zoom
            else -> return null
        }
        return PathEffect.dashPathEffect(floatArrayOf(16f * zoom * density, 8f * zoom * density), phase * density)
    }

    private fun DrawScope.drawArcBetweenTwoPoints(
        a: Offset,
        b: Offset,
        radius: Float,
        brush: Brush,
        style: DrawStyle,
    ) {
        val x = b.x - a.x
        val y = b.y - a.y
        val angle = atan2(y, x)
        val l = sqrt((x * x + y * y))
        if (2 * radius >= l) {
            val sweep = asin(l / (2 * radius))
            val h = radius * cos(sweep)
            val c = Offset(
                x = (a.x + x / 2 - h * (y / l)),
                y = (a.y + y / 2 + h * (x / l)),
            )
            val sweepAngle = Math.toDegrees((2 * sweep).toDouble()).toFloat()
            drawArc(
                brush = brush,
                topLeft = Offset(c.x - radius, c.y - radius),
                size = Size(radius * 2, radius * 2),
                startAngle = (Math.toDegrees((angle - sweep).toDouble()) - 90).toFloat(),
                sweepAngle = sweepAngle,
                useCenter = false,
                style = style,
            )
        }
    }

    private fun DrawScope.getCanvasCoordinates(x: Int, y: Int, center: DoubleOffset, scale: Float): Offset {
        val canvasX = (x - center.x) / scale + size.center.x
        val canvasY = (y - center.y) / scale + size.center.y
        return Offset(canvasX.toFloat(), canvasY.toFloat())
    }

    private fun DrawScope.isOnCanvas(offset: Offset, margin: Int = 0): Boolean {
        return offset.x >= -margin && offset.y >= -margin && offset.x < size.width + margin && offset.y < size.height + margin
    }
}
