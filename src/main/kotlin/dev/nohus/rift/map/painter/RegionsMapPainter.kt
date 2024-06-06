package dev.nohus.rift.map.painter

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import dev.nohus.rift.map.DoubleOffset
import dev.nohus.rift.map.MapViewModel.Cluster
import dev.nohus.rift.map.MapViewModel.Layout
import dev.nohus.rift.map.systemcolor.RegionColors
import dev.nohus.rift.map.systemcolor.SystemColorStrategy

class RegionsMapPainter(
    private val cluster: Cluster,
    private val layout: Map<Int, Layout>,
) : MapPainter {

    private lateinit var textMeasurer: TextMeasurer
    private val regionNames = cluster.regions.associate { it.id to it.name }
    private val connectionsInLayout = getRegionConnections()
    private var connectionColor: Color = Color.Unspecified

    data class RegionConnection(
        val from: Int,
        val to: Int,
    )

    private fun getRegionConnections(): List<RegionConnection> {
        return cluster.connections.mapNotNull { connection ->
            if (connection.from.regionId == connection.to.regionId) return@mapNotNull null
            val (from, to) = listOf(connection.from.regionId, connection.to.regionId).sorted()
            RegionConnection(from, to)
        }.distinct()
    }

    @Composable
    override fun initializeComposed() {
        textMeasurer = rememberTextMeasurer()
        connectionColor = Color.White
    }

    override fun drawStatic(
        scope: DrawScope,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        systemColorStrategy: SystemColorStrategy,
        cellColorStrategy: SystemColorStrategy?,
    ) = with(scope) {
        connectionsInLayout.forEach { connection ->
            drawRegionConnection(connection, center, scale)
        }
    }

    override fun drawAnimated(
        scope: DrawScope,
        center: DoubleOffset,
        scale: Float,
        zoom: Float,
        animationPercentage: Float,
        systemColorStrategy: SystemColorStrategy,
    ) {}

    private fun DrawScope.drawRegionConnection(
        connection: RegionConnection,
        center: DoubleOffset,
        scale: Float,
    ) {
        val fromLayoutPosition = layout[connection.from]!!.position
        val toLayoutPosition = layout[connection.to]!!.position
        val from = getCanvasCoordinates(fromLayoutPosition.x, fromLayoutPosition.y, center, scale)
        val to = getCanvasCoordinates(toLayoutPosition.x, toLayoutPosition.y, center, scale)
        val deltaOffset = to - from

        val fromColor = RegionColors.getColor(regionNames[connection.from]!!)
        val toColor = RegionColors.getColor(regionNames[connection.to]!!)
        val brush = Brush.linearGradient(listOf(fromColor, toColor), start = Offset.Zero, end = deltaOffset)

        translate(from.x, from.y) {
            val width = (1f / scale).coerceAtMost(2f) * density
            drawLine(brush, start = Offset.Zero, end = deltaOffset, strokeWidth = width)
        }
    }

    private fun DrawScope.getCanvasCoordinates(x: Int, y: Int, center: DoubleOffset, scale: Float): Offset {
        val canvasX = (x - center.x) / scale + size.center.x
        val canvasY = (y - center.y) / scale + size.center.y
        return Offset(canvasX.toFloat(), canvasY.toFloat())
    }
}
