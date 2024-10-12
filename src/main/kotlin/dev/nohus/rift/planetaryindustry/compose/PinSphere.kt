package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.scale
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.pi_circle_command
import dev.nohus.rift.generated.resources.pi_circle_extractor
import dev.nohus.rift.generated.resources.pi_circle_process
import dev.nohus.rift.generated.resources.pi_circle_processadvanced
import dev.nohus.rift.generated.resources.pi_circle_processhightech
import dev.nohus.rift.generated.resources.pi_circle_spaceport
import dev.nohus.rift.generated.resources.pi_circle_storage
import dev.nohus.rift.generated.resources.pi_cycle_10px
import dev.nohus.rift.generated.resources.pi_disc_shadow
import dev.nohus.rift.generated.resources.pi_gauge_15px
import dev.nohus.rift.generated.resources.pi_gauge_20px
import dev.nohus.rift.generated.resources.pi_hash15px
import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorExpired
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorInactive
import dev.nohus.rift.planetaryindustry.models.PinStatus.InputNotRouted
import dev.nohus.rift.planetaryindustry.models.PinStatus.NotSetup
import dev.nohus.rift.planetaryindustry.models.PinStatus.OutputNotRouted
import dev.nohus.rift.planetaryindustry.models.getCapacity
import dev.nohus.rift.planetaryindustry.models.getName
import dev.nohus.rift.utils.formatNumberCompact
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import java.time.Instant

private val saveLayerPaint = Paint()
private val storageUsageTint = ColorFilter.tint(Color(0xFF00a9f4), BlendMode.Modulate)
private val factoryInputsTint = ColorFilter.tint(Color(0xFFed9935), BlendMode.Modulate)
private val needsAttentionTint = ColorFilter.tint(Color(0xFFFF0000))

@Composable
fun PinSphere(
    colony: Colony,
    pin: Pin,
    size: Dp,
    now: Instant,
    isAdvancingTime: Boolean,
    onRequestSimulation: () -> Unit,
) {
    val pxSize = LocalDensity.current.run { size.toPx().toInt() }
    val shadow = imageResource(Res.drawable.pi_disc_shadow)
    val iconSize = ((125f / 256f) * pxSize).toInt()
    val icon = imageResource(getIcon(pin), iconSize)
    val gauge = imageResource(Res.drawable.pi_gauge_15px, pxSize)
    val attentionGauge = imageResource(Res.drawable.pi_gauge_20px)
    val cpuPowerDiameter = (((256f - 45f) / 256f) * pxSize).toInt()
    val cpuPower = imageResource(Res.drawable.pi_hash15px, cpuPowerDiameter)
    val cycleDiameter = (pxSize * 0.7).toInt()
    val cycle = imageResource(Res.drawable.pi_cycle_10px, cycleDiameter)

    val needsAttention by derivedStateOf {
        pin.status in listOf(NotSetup, InputNotRouted, OutputNotRouted, ExtractorExpired, ExtractorInactive)
    }
    val loadAnimation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        loadAnimation.animateTo(1f, animationSpec = spring(stiffness = StiffnessLow))
    }
    val infiniteTransition = rememberInfiniteTransition()
    val pulsingCycleAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val pulsingAttentionAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val pulsingAttentionScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    RiftTooltipArea(
        tooltip = { Tooltip(colony, pin, now, isAdvancingTime, onRequestSimulation) },
    ) {
        Canvas(
            modifier = Modifier.size(size, size),
        ) {
            translate(this.size.width / 2, this.size.height / 2) {
                val diameter = (this.size.width * 1.2).toInt()
                drawImage(
                    image = shadow,
                    dstSize = IntSize(diameter, diameter),
                    dstOffset = IntOffset(-diameter / 2, -diameter / 2),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.05f)),
                )
            }

            drawIcon(icon, pin)

            if (pin is Pin.Factory) {
                drawFactoryInputs(pin, gauge, loadAnimation.value)
            } else {
                drawOuterGauge(gauge, pin)
            }

            if (pin is Pin.Storage || pin is Pin.Launchpad || pin is Pin.CommandCenter) {
                drawStorageUsage(pin, gauge, loadAnimation.value)
            }

            if (pin is Pin.CommandCenter) {
                drawCpuPowerUsage(cpuPower, colony)
            }

            if (pin is Pin.Factory) {
                val lastRunTime = pin.lastRunTime
                if (pin.isActive && lastRunTime != null && pin.schematic != null) {
                    val cycleProgressDuration = colony.currentSimTime.toEpochMilli() - lastRunTime.toEpochMilli()
                    val totalCycleDuration = pin.schematic.cycleTime.toMillis()
                    drawCycle(cycle, cycleProgressDuration, totalCycleDuration, loadAnimation.value)
                } else if (pin.schematic != null) {
                    drawPulsingCycle(cycle, pulsingCycleAlpha)
                }
            }

            if (pin is Pin.Extractor) {
                if (pin.isActive && pin.cycleTime != null && pin.installTime != null) {
                    val elapsedTime = colony.currentSimTime.toEpochMilli() - pin.installTime.toEpochMilli()
                    val totalCycleDuration = pin.cycleTime.toMillis()
                    val cycleProgressDuration = elapsedTime % totalCycleDuration
                    drawCycle(cycle, cycleProgressDuration, totalCycleDuration, loadAnimation.value)
                }
            }

            if (needsAttention) {
                drawNeedsAttentionGauge(attentionGauge, pulsingAttentionScale, pulsingAttentionAlpha)
            }
        }
    }
}

@Composable
private fun Tooltip(
    colony: Colony,
    pin: Pin,
    now: Instant,
    isAdvancingTime: Boolean,
    onRequestSimulation: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(Spacing.large),
    ) {
        Text(
            text = pin.getName(),
            style = RiftTheme.typography.titlePrimary,
        )
        when (pin) {
            is Pin.CommandCenter -> {
                Spacer(Modifier.height(Spacing.verySmall))
                CommandCenter(pin, colony.usage)
            }
            is Pin.Extractor -> {
                Spacer(Modifier.height(Spacing.verySmall))
                Extractor(now, pin, colony.routes, isAdvancingTime, onRequestSimulation)
            }
            is Pin.Factory -> {
                Factory(now, pin, colony.routes, isAdvancingTime, onRequestSimulation)
            }
            is Pin.Launchpad -> {
                Storage(pin.contents, pin.capacityUsed, pin.getCapacity()!!.toFloat())
            }
            is Pin.Storage -> {
                Storage(pin.contents, pin.capacityUsed, pin.getCapacity()!!.toFloat())
            }
        }
    }
}

@Composable
private fun StorageTooltip(pin: Pin) {
    if (pin.contents.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            pin.contents.forEach { (type, quantity) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${formatNumberCompact(quantity)} x",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    AsyncTypeIcon(
                        type = type,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        val capacity = pin.getCapacity()!!
        Text(String.format("%.0f/%.0f m3", pin.capacityUsed, capacity.toFloat()))
    } else {
        Text(
            text = "Storage empty",
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

private fun DrawScope.drawNeedsAttentionGauge(
    attentionGauge: ImageBitmap,
    pulsingAttentionScale: Float,
    pulsingAttentionAlpha: Float,
) {
    val diameter = (size.width * 1.0106).toInt()
    scale(pulsingAttentionScale, Offset(size.width / 2, size.height / 2)) {
        drawImage(
            image = attentionGauge,
            dstSize = IntSize(diameter, diameter),
            colorFilter = needsAttentionTint,
            alpha = pulsingAttentionAlpha,
        )
    }
}

private fun DrawScope.drawPulsingCycle(
    cycle: ImageBitmap,
    pulsingCycleAlpha: Float,
) {
    translate(size.width / 2, size.height / 2) {
        val diameter = (size.width * 0.7).toInt()
        drawImage(
            image = cycle,
            dstOffset = IntOffset(-diameter / 2, -diameter / 2),
            alpha = pulsingCycleAlpha,
        )
    }
}

private fun DrawScope.drawCycle(
    cycle: ImageBitmap,
    progress: Long,
    total: Long,
    loadAnimation: Float,
) {
    val percentage = (progress / total.toFloat()).coerceIn(0f, 1f) * loadAnimation
    val percentageLeft = 1f - percentage
    drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
        translate(size.width / 2, size.height / 2) {
            val diameter = (size.width * 0.7).toInt()
            drawImage(
                image = cycle,
                dstOffset = IntOffset(-diameter / 2, -diameter / 2),
            )
        }

        drawArc(
            color = Color.Transparent,
            startAngle = -90f + (percentage * 360f),
            sweepAngle = percentageLeft * 360f,
            useCenter = true,
            blendMode = BlendMode.Clear,
        )
    }
}

private fun DrawScope.drawCpuPowerUsage(
    cpuPower: ImageBitmap,
    colony: Colony,
) {
    drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
        translate(size.width / 2, size.height / 2) {
            val diameter = (((256f - 45f) / 256f) * size.width).toInt()
            drawImage(
                image = cpuPower,
                dstOffset = IntOffset(-diameter / 2, -diameter / 2),
            )
        }

        drawArc(
            color = Color(0xFF590000),
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = true,
            blendMode = BlendMode.SrcIn,
        )
        val percentagePower = colony.usage.let { it.powerUsage / it.powerSupply.toFloat() }
        drawArc(
            color = Color(0xFFec1c24),
            startAngle = -90f,
            sweepAngle = 180f * percentagePower,
            useCenter = true,
            blendMode = BlendMode.SrcIn,
        )

        drawArc(
            color = Color(0xFF005444),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true,
            blendMode = BlendMode.SrcIn,
        )
        val percentageCpu = colony.usage.let { it.cpuUsage / it.cpuSupply.toFloat() }
        drawArc(
            color = Color(0xFF00ffd4),
            startAngle = 90f,
            sweepAngle = 180f * percentageCpu,
            useCenter = true,
            blendMode = BlendMode.SrcIn,
        )
    }
}

private fun DrawScope.drawStorageUsage(
    pin: Pin,
    gauge: ImageBitmap,
    loadAnimation: Float,
) {
    val capacity = pin.getCapacity()!!
    val percentageUsed = (pin.capacityUsed / capacity) * loadAnimation
    val percentageFree = 1f - percentageUsed
    drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
        drawImage(
            image = gauge,
            colorFilter = storageUsageTint,
        )

        drawArc(
            color = Color.Transparent,
            startAngle = -90f + (percentageUsed * 360f),
            sweepAngle = percentageFree * 360f,
            useCenter = true,
            blendMode = BlendMode.Clear,
        )
    }
}

private fun DrawScope.drawFactoryInputs(
    pin: Pin.Factory,
    gauge: ImageBitmap,
    loadAnimation: Float,
) {
    val inputs = pin.schematic?.inputs?.size ?: 0
    drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
        drawOuterGauge(gauge, pin)

        drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
            drawImage(
                image = gauge,
                colorFilter = factoryInputsTint,
            )

            pin.schematic?.inputs?.entries?.forEachIndexed { index, (type, requiredAmount) ->
                val currentAmount = pin.contents[type] ?: 0
                val percent = (currentAmount / requiredAmount.toFloat()).coerceIn(0f, 1f) * loadAnimation
                val percentMissing = 1f - percent
                val sliceAngle = 360f / inputs
                drawArc(
                    color = Color.Transparent,
                    startAngle = -90f + sliceAngle * index + sliceAngle * percent,
                    sweepAngle = sliceAngle * percentMissing,
                    useCenter = true,
                    blendMode = BlendMode.Clear,
                )
            }
        }

        if (inputs > 1) {
            repeat(inputs) { index ->
                val gapAngle = 14.4f
                drawArc(
                    color = Color.Transparent,
                    startAngle = -90f + (360f / inputs) * index - (gapAngle / 2),
                    sweepAngle = gapAngle,
                    useCenter = true,
                    blendMode = BlendMode.Clear,
                )
            }
        }
    }
}

private fun DrawScope.drawOuterGauge(
    gauge: ImageBitmap,
    pin: Pin,
) {
    drawImage(
        image = gauge,
        colorFilter = getGaugeColorFilter(pin),
    )
}

private fun DrawScope.drawIcon(
    icon: ImageBitmap,
    pin: Pin,
) {
    translate(size.width / 2, size.height / 2) {
        val iconSize = (125f / 256f) * size.width
        drawImage(
            image = icon,
            dstOffset = IntOffset(-iconSize.toInt() / 2, -iconSize.toInt() / 2),
            colorFilter = getIconColorFilter(pin),
        )
    }
}

private val gaugeColorFilterCache = mutableMapOf<String, ColorFilter>()
private fun getGaugeColorFilter(pin: Pin): ColorFilter {
    gaugeColorFilterCache[pin.name]?.let { return it }
    val color = when (pin) {
        is Pin.CommandCenter -> Color(0xFF0a4366)
        is Pin.Extractor -> Color(0xFF009999)
        is Pin.Factory -> {
            if ("Advanced" in pin.name) {
                Color(0xFFeddb35)
            } else if ("High-Tech" in pin.name) {
                Color(0xFFbced35)
            } else {
                Color(0xFFb2622d)
            }
        }
        is Pin.Launchpad -> Color(0xFF0a4366)
        is Pin.Storage -> Color(0xFF0a4366)
    }
    return ColorFilter.tint(color, BlendMode.Modulate).also { gaugeColorFilterCache[pin.name] = it }
}

private val iconColorFilterCache = mutableMapOf<String, ColorFilter>()
private fun getIconColorFilter(pin: Pin): ColorFilter {
    iconColorFilterCache[pin.name]?.let { return it }
    val color = when (pin) {
        is Pin.CommandCenter -> Color(0xFFabf3ff)
        is Pin.Extractor -> Color(0xFF30efd8)
        is Pin.Factory -> {
            if ("Advanced" in pin.name) {
                Color(0xFFeddb35)
            } else if ("High-Tech" in pin.name) {
                Color(0xFFbced35)
            } else {
                Color(0xFFed9935)
            }
        }
        is Pin.Launchpad -> Color(0xFF03b2ef)
        is Pin.Storage -> Color(0xFF026a93)
    }
    return ColorFilter.tint(color, BlendMode.Modulate).also { iconColorFilterCache[pin.name] = it }
}

private fun getIcon(pin: Pin): DrawableResource {
    return when {
        "Extractor Control Unit" in pin.name -> Res.drawable.pi_circle_extractor
        "Production Plant" in pin.name -> Res.drawable.pi_circle_processhightech
        "Advanced Industry Facility" in pin.name -> Res.drawable.pi_circle_processadvanced
        "Industry Facility" in pin.name -> Res.drawable.pi_circle_process
        "Command Center" in pin.name -> Res.drawable.pi_circle_command
        "Launchpad" in pin.name -> Res.drawable.pi_circle_spaceport
        "Storage" in pin.name -> Res.drawable.pi_circle_storage
        else -> Res.drawable.pi_circle_command
    }
}

private val imageCache = mutableMapOf<Pair<DrawableResource, Int>, ImageBitmap>()

/**
 * Version of imageResource that will do a high quality rescale of the image and cache the result
 */
@Composable
private fun imageResource(resource: DrawableResource, sizeToScale: Int): ImageBitmap {
    val original = imageResource(resource)
    val scaled = remember {
        imageCache[resource to sizeToScale]?.let { return@remember it }
        original.scale(sizeToScale, sizeToScale).also { imageCache[resource to sizeToScale] = it }
    }
    return scaled
}
