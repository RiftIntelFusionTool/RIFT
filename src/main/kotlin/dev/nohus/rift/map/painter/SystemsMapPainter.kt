package dev.nohus.rift.map.painter

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
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

class SystemsMapPainter(
    private val cluster: Cluster,
    private val layout: Map<Int, Position>,
    private val solarSystemColorStrategy: SolarSystemColorStrategy,
    private val mapType: MapType,
) : MapPainter {

    private lateinit var textMeasurer: TextMeasurer
    private lateinit var regionNameStyle: TextStyle
    private val systemsIdsInLayout = layout.keys
    private val systemsInLayout = cluster.systems.filter {
        it.id in systemsIdsInLayout
    }
    private val connectionsInLayout = cluster.connections.filter {
        it.from.id in systemsIdsInLayout && it.to.id in systemsIdsInLayout
    }
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
    ) = with(scope) {
        connectionsInLayout.forEach { connection ->
            drawSystemConnection(connection, mapType, center, scale, zoom)
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
        system: SolarSystemsRepository.MapSolarSystem,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
    ) {
        val position = MapLayoutRepository.transformNewEdenCoordinate(system.x, system.z)
        val offset = getCanvasCoordinates(position.x, position.y, center, scale)
        if (isOnCanvas(offset, 100)) {
            val systemColor = solarSystemColorStrategy.getActiveColor(system)
            val radius = 4f
            val brush = systemRadialGradients.getOrPut(systemColor) {
                Brush.radialGradient(
                    listOf(systemColor.copy(alpha = 0.7f), systemColor.copy(alpha = 0f)),
                    radius = radius,
                    center = Offset.Zero,
                )
            }
            translate(offset.x, offset.y) {
                drawCircle(brush, radius = radius, center = Offset.Zero)
                drawCircle(systemColor, radius = (0.2f * zoom / 2), center = Offset.Zero)
            }
        }
    }

    private fun DrawScope.drawSystemConnection(
        connection: GateConnection,
        mapType: MapType,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
    ) {
        val fromLayoutPosition = layout[connection.from.id]!!
        val toLayoutPosition = layout[connection.to.id]!!
        val from = getCanvasCoordinates(fromLayoutPosition.x, fromLayoutPosition.y, center, scale)
        val to = getCanvasCoordinates(toLayoutPosition.x, toLayoutPosition.y, center, scale)
        val deltaOffset = to - from
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
            MapGateConnectionsRepository.ConnectionType.Constellation -> PathEffect.dashPathEffect(floatArrayOf(6.0f * zoom, 2.0f * zoom))
            MapGateConnectionsRepository.ConnectionType.Region -> PathEffect.dashPathEffect(floatArrayOf(15.0f * zoom, 5.0f * zoom))
        }
        if (connection.type == MapGateConnectionsRepository.ConnectionType.Region || scale < 4) {
            translate(from.x, from.y) {
                val width = (1f / scale).coerceAtMost(2f)
                drawLine(brush, start = Offset.Zero, end = deltaOffset, strokeWidth = width, pathEffect = effect)
            }
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
