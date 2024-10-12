package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.animation.core.Spring.StiffnessVeryLow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.utils.formatDurationNumeric
import java.time.Duration
import java.time.Instant

@Composable
fun AnnotatedProgressBar(
    title: String,
    duration: Duration,
    totalDuration: Duration,
    color: Color,
    titleStyle: TextStyle = RiftTheme.typography.bodyPrimary,
    isAdvancingTime: Boolean,
    onFinished: () -> Unit,
) {
    val initialTime = remember(duration) { Instant.now() }
    val elapsedRealTime = if (isAdvancingTime) Duration.between(initialTime, getNow()) else Duration.ZERO
    val tickingDuration = (duration + elapsedRealTime).coerceAtMost(totalDuration)
    val percentage = tickingDuration.seconds / totalDuration.seconds.toFloat()
    LaunchedEffect(tickingDuration) {
        if (tickingDuration >= totalDuration) onFinished()
    }
    AnnotatedProgressBar(
        title = title,
        percentage = percentage,
        description = "${formatDurationNumeric(tickingDuration)} / ${formatDurationNumeric(totalDuration)}",
        color = color,
        titleStyle = titleStyle,
    )
}

@Composable
fun AnnotatedProgressBar(
    title: String,
    percentage: Float,
    description: String,
    color: Color,
    titleStyle: TextStyle = RiftTheme.typography.bodyPrimary,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
    ) {
        Text(
            text = title,
            style = titleStyle,
        )
        ProgressBar(
            percentage = percentage.coerceIn(0f, 1f),
            color = color,
        )
        Text(
            text = description,
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}

@Composable
private fun ProgressBar(
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val width = 144.dp
    val height = 6.dp
    Box(
        modifier = modifier
            .size(width, height)
            .background(RiftTheme.colors.progressBarBackground),
    ) {
        var animatedPercentage by remember { mutableStateOf(0f) }
        val progressWidth by animateDpAsState(animatedPercentage * width, spring(stiffness = StiffnessVeryLow))
        LaunchedEffect(percentage) { animatedPercentage = percentage }
        Box(
            modifier = Modifier
                .size(progressWidth, height)
                .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal))
                .background(color),
        ) {}
        Box(
            modifier = Modifier
                .size(progressWidth, height)
                .background(color),
        ) {}
    }
}
