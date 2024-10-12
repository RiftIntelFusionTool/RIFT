package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.Anchor
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.utils.formatDurationCompact
import dev.nohus.rift.utils.formatNumber
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.max

private val colors = listOf(
    0.0f to Color(0xFF3D90CC),
    0.1f to Color(0xFF3DB6CC),
    0.2f to Color(0xFF3DCCBE),
    0.3f to Color(0xFF3DCC8E),
    0.4f to Color(0xFF3DCC5C),
    0.5f to Color(0xFF4FCC3D),
    0.6f to Color(0xFF82CC3D),
    0.7f to Color(0xFFB6CC3D),
    0.8f to Color(0xFFCCA83D),
    0.9f to Color(0xFFCC7E3D),
    1.0f to Color(0xFFCC3D3D),
)
private const val legendLineWidth = 4f
private const val leftTextWidth = 50f
private const val verticalTextHeight = 30f
private const val graphWidth = 337f
private const val graphHeight = 214f
private const val barsPadding = 2f
private val fullWidth = (graphWidth + leftTextWidth + legendLineWidth).dp
private val fullHeight = (graphHeight + verticalTextHeight + legendLineWidth).dp

/**
 * Extraction bar graph + animations + tooltip
 */
@Composable
fun ExtractionBarGraph(
    prediction: List<Long>,
    totalProgramDuration: Duration,
    elapsedTime: Duration,
) {
    var tooltipOffset by remember { mutableStateOf(Offset.Zero) }
    var tooltipText by remember { mutableStateOf<AnnotatedString?>(null) }
    val tooltipPrimary = RiftTheme.colors.textPrimary
    val tooltipSecondary = RiftTheme.colors.textSecondary
    val tooltipFontSize = RiftTheme.typography.titlePrimary.fontSize
    RiftTooltipArea(
        text = tooltipText,
        contentAnchor = Anchor.Left,
        horizontalOffset = LocalDensity.current.run { tooltipOffset.x.toDp() },
        verticalOffset = LocalDensity.current.run { tooltipOffset.y.toDp() },
    ) {
        ExtractionBarGraph(
            values = prediction,
            programDuration = totalProgramDuration,
            currentDuration = elapsedTime,
            onCycleHover = { cycle, offset ->
                if (cycle != null) {
                    tooltipOffset = offset
                    tooltipText = buildAnnotatedString {
                        withStyle(SpanStyle(color = tooltipPrimary, fontSize = tooltipFontSize)) {
                            appendLine("Cycle ${cycle + 1}")
                            appendLine("${formatNumber(prediction[cycle])} units")
                        }
                        withStyle(SpanStyle(color = tooltipSecondary, fontSize = tooltipFontSize)) {
                            appendLine("Accumulated:")
                            appendLine("${formatNumber(prediction.take(cycle + 1).sum())} units")
                            val cycleTime = totalProgramDuration.dividedBy(prediction.size.toLong())
                            append(formatDurationCompact(cycleTime.multipliedBy(cycle + 1L)))
                        }
                    }
                } else {
                    tooltipText = null
                }
            },
        )
    }
}

/**
 * Extraction bar graph + animations
 */
@Composable
private fun ExtractionBarGraph(
    values: List<Long>,
    programDuration: Duration,
    currentDuration: Duration,
    onCycleHover: (cycle: Int?, offset: Offset) -> Unit,
) {
    val animatedValues by remember(values) { mutableStateOf(List(values.size) { Animatable(0f) }) }
    LaunchedEffect(values) {
        val delay = 20L
        coroutineScope {
            animatedValues.forEachIndexed { index, animatedValue ->
                launch {
                    delay(delay * index)
                    animatedValue.animateTo(values[index].toFloat(), animationSpec = tween(500))
                }
            }
        }
    }

    val maxValue = remember(values) {
        values.max().let {
            when {
                it < 1000 -> (it / 100) * 100 + 100
                it < 10_000 -> (it / 1_000) * 1_000 + 1_000
                it < 100_000 -> (it / 10_000) * 10_000 + 10_000
                else -> (it / 100_000) * 100_000 + 100_000
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val currentDurationColor by infiniteTransition.animateColor(
        initialValue = Color.White,
        targetValue = Color.White.copy(alpha = 0.5f),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    ExtractionBarGraph(
        values = animatedValues.map { it.value.toLong() },
        maxValue = maxValue,
        programDuration = programDuration,
        currentDuration = currentDuration,
        currentDurationColor = currentDurationColor,
        onCycleHover = onCycleHover,
    )
}

/**
 * Extraction bar graph, static
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ExtractionBarGraph(
    values: List<Long>,
    maxValue: Long,
    programDuration: Duration,
    currentDuration: Duration,
    currentDurationColor: Color,
    onCycleHover: (cycle: Int?, offset: Offset) -> Unit,
) {
    val legendStyle = RiftTheme.typography.bodyPrimary
    val borderColor = RiftTheme.colors.textPrimary
    val backgroundLinesColor = RiftTheme.colors.windowBackground
    val backgroundColor = RiftTheme.colors.windowBackgroundSecondary

    val totalTimeText = remember(programDuration) { formatDurationCompact(programDuration) }
    val yLegend = remember(maxValue) {
        listOf(1.0, 3 / 4.0, 2 / 4.0, 1 / 4.0, 0.0).map {
            formatNumber((it * maxValue).toInt())
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val yLegendMeasured = yLegend.map { textMeasurer.measure(it, legendStyle) }
    val barWidth = max(2, ((graphWidth - barsPadding * 2) / values.size).toInt() - 1).toFloat()
    val barsWidth = values.size * (barWidth + 1f) - 1f
    val barsEnd = (2 * barsPadding) + barsWidth
    val firstBarOffset = leftTextWidth + legendLineWidth + barsPadding
    var hoveredCycle by remember { mutableStateOf<Int?>(null) }

    val borderPaint = remember(borderColor) {
        Paint().apply {
            color = borderColor
            isAntiAlias = false
            strokeWidth = 1f
            style = PaintingStyle.Stroke
        }
    }
    val backgroundPaint = remember(backgroundColor) {
        Paint().apply {
            color = backgroundColor
            isAntiAlias = false
            strokeWidth = 1f
        }
    }
    val backgroundLinesPaint = remember(backgroundLinesColor) {
        Paint().apply {
            color = backgroundLinesColor
            isAntiAlias = false
            strokeWidth = 1f
        }
    }
    val barPaint = remember(barWidth) {
        Paint().apply {
            isAntiAlias = false
            strokeWidth = barWidth
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 1f))
        }
    }
    val barCapPaint = remember(barWidth) {
        Paint().apply {
            isAntiAlias = false
            strokeWidth = barWidth
        }
    }
    val currentTimePaint = remember {
        Paint().apply {
            isAntiAlias = false
            strokeWidth = 1f
        }
    }
    val hoveredCyclePaint = remember(barWidth) {
        Paint().apply {
            color = Color.Black.copy(alpha = 0.5f)
            isAntiAlias = false
            strokeWidth = barWidth
        }
    }

    Canvas(
        modifier = Modifier
            .size(fullWidth, fullHeight)
            .onPointerEvent(PointerEventType.Move) {
                it.changes.forEach { change ->
                    val cycle = (change.position.x - firstBarOffset) / (barWidth + 1f)
                    hoveredCycle = cycle.toInt().takeIf { it in 0..values.lastIndex }
                    val offset = Offset(change.position.x, yLegendMeasured[0].lastBaseline)
                    onCycleHover(hoveredCycle, offset)
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoveredCycle = null
                onCycleHover(null, Offset.Zero)
            },
    ) {
        // Y axis legend
        yLegendMeasured.forEachIndexed { index, text ->
            translate(left = leftTextWidth - text.size.width, top = index * (graphHeight / yLegend.lastIndex)) {
                drawText(text)
                translate(left = text.size.width.toFloat() - 1f, top = text.lastBaseline) {
                    drawContext.canvas.drawLine(
                        p1 = Offset(0f, 0f),
                        p2 = Offset(legendLineWidth, 0f),
                        paint = borderPaint,
                    )
                }
            }
        }

        // X axis legend
        translate(left = leftTextWidth + legendLineWidth, top = yLegendMeasured[0].lastBaseline + graphHeight) {
            val measuredStart = textMeasurer.measure("Program start", legendStyle)
            val measuredEnd = textMeasurer.measure(totalTimeText, legendStyle)
            translate(top = legendLineWidth) {
                drawText(measuredStart)
                translate(left = barsEnd - measuredEnd.size.width) {
                    drawText(measuredEnd)
                }
            }
            drawContext.canvas.drawLine(
                p1 = Offset(0f, 0f),
                p2 = Offset(0f, legendLineWidth),
                paint = borderPaint,
            )
            drawContext.canvas.drawLine(
                p1 = Offset(barsEnd, 0f),
                p2 = Offset(barsEnd, legendLineWidth),
                paint = borderPaint,
            )
        }

        translate(left = leftTextWidth + legendLineWidth, top = yLegendMeasured[0].lastBaseline) {
            // Graph background
            drawContext.canvas.drawRect(
                left = 0f,
                top = 0f,
                right = graphWidth,
                bottom = graphHeight,
                paint = backgroundPaint,
            )

            // Graph border
            drawContext.canvas.drawRect(
                left = 0f,
                top = 0f,
                right = graphWidth,
                bottom = graphHeight,
                paint = borderPaint,
            )

            // Graph background lines
            for (y in 2 until graphHeight.toInt() step 2) {
                drawContext.canvas.drawLine(
                    p1 = Offset(0f, y.toFloat()),
                    p2 = Offset(graphWidth - 1f, y.toFloat()),
                    paint = backgroundLinesPaint,
                )
            }

            // Bars
            val bottom = graphHeight - 1f
            translate(left = barsPadding) {
                values.forEachIndexed { index, value ->
                    val percent = value.toFloat() / maxValue
                    val height = (graphHeight * percent).toInt().toFloat()
                    val x = (index * (barWidth + 1f)) + (barWidth / 2f)

                    var twoColors = colors.dropWhile { it.first < percent }.take(2)
                    if (twoColors.size < 2) twoColors = colors.takeLast(2)
                    val capColor = lerp(twoColors[0].second, twoColors[1].second, (percent - twoColors[0].first) / 0.1f)
                    val color = capColor.copy(alpha = 0.5f)

                    drawContext.canvas.drawLine(
                        p1 = Offset(x, bottom),
                        p2 = Offset(x, bottom - height),
                        paint = barPaint.apply {
                            val fraction = ((percent - 0.25f).coerceAtLeast(0f) * (1 + 1 / 3f)).coerceIn(0f, 1f)
                            val color1 = lerp(color, Color.Transparent, fraction)
                            shader = LinearGradientShader(
                                from = Offset(x, bottom),
                                to = Offset(x, bottom - height),
                                colors = listOf(color1, color, color),
                            )
                        },
                    )
                    var capY = (bottom - height).toInt()
                    if (capY % 2 == 0) capY--
                    drawContext.canvas.drawLine(
                        p1 = Offset(x, capY.toFloat()),
                        p2 = Offset(x, capY - 1f),
                        paint = barCapPaint.apply {
                            this.color = capColor
                        },
                    )
                }
            }

            // Current time indicator
            val percent = currentDuration.seconds / programDuration.seconds.toFloat()
            val x = percent * barsEnd
            drawContext.canvas.drawLine(
                p1 = Offset(x, bottom),
                p2 = Offset(x, 0f),
                paint = currentTimePaint.apply {
                    color = currentDurationColor
                },
            )

            // Hovered cycle indicator
            hoveredCycle?.let { hoveredCycle ->
                val x = barsPadding + (hoveredCycle * (barWidth + 1f)) + (barWidth / 2f)
                drawContext.canvas.drawLine(
                    p1 = Offset(x, bottom),
                    p2 = Offset(x, 0f),
                    paint = hoveredCyclePaint,
                )
            }
        }
    }
}
