package dev.nohus.rift.map.systemcolor

import androidx.collection.MutableIntLongMap
import androidx.compose.ui.graphics.Color
import dev.nohus.rift.network.imageserver.ImageServerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.annotation.Single
import java.awt.image.BufferedImage

@Single
class SovereigntyColorRepository(
    private val imageServerApi: ImageServerApi,
) {

    private val scope = CoroutineScope(SupervisorJob())

    private val factionColors = mapOf(
        500001 to Color(0xFF9AD2E3), // Caldari State
        500002 to Color(0xFF9D452D), // Minmatar Republic
        500003 to Color(0xFFFFEE93), // Amarr Empire
        500004 to Color(0xFF6DB09E), // Gallente Federation
        500005 to Color(0xFF063C26), // Jove Empire
    )
    private val colorsByIds = MutableIntLongMap()
    private val processedIds = mutableSetOf<Int>()

    fun getFactionColor(factionId: Int): Color {
        factionColors[factionId]?.let { return it }
        if (colorsByIds.containsKey(factionId)) return Color(colorsByIds[factionId].toULong())
        getColorFromLogo(factionId, imageServerApi::getCorporationLogo)
        return Color.Unspecified
    }

    fun getAllianceColor(allianceId: Int): Color {
        if (colorsByIds.containsKey(allianceId)) return Color(colorsByIds[allianceId].toULong())
        getColorFromLogo(allianceId, imageServerApi::getAllianceLogo)
        return Color.Unspecified
    }

    private fun getColorFromLogo(id: Int, getLogo: suspend (id: Int, size: Int) -> BufferedImage?) {
        if (id in processedIds) return
        processedIds += id

        scope.launch(Dispatchers.Default) {
            val logo = getLogo(id, 32) ?: run {
                processedIds -= id
                return@launch
            }
            val color = getDominantColor(logo).value.toLong()
            withContext(MainUIDispatcher) {
                colorsByIds[id] = color
            }
        }
    }

    private fun getPixels(image: BufferedImage): List<DoublePoint> {
        return mutableListOf<DoublePoint>().also {
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    if (image.isTransparent(x, y)) continue
                    val color = Color(image.getRGB(x, y))
                    it.add(DoublePoint(doubleArrayOf(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())))
                }
            }
        }
    }

    private fun getDominantColor(image: BufferedImage): Color {
        return KMeansPlusPlusClusterer<DoublePoint>(5)
            .cluster(getPixels(image))
            .mapNotNull { cluster ->
                cluster?.center?.point?.map { it.toFloat() } // Center of the pixel cluster
            }.map { point ->
                Color(point[0], point[1], point[2])
            }.maxByOrNull {
                it.getIntensity()
            } ?: Color.Unspecified
    }

    private fun BufferedImage.isTransparent(x: Int, y: Int): Boolean {
        return alphaRaster?.getPixel(x, y, IntArray(1))?.first() == 0
    }

    /**
     * Returns the smaller of HSV saturation (more color) and value (less blackness)
     */
    private fun Color.getIntensity(): Float {
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val delta = max - min
        val saturation = if (max == 0f) 0f else delta / max
        return minOf(saturation, max)
    }
}
