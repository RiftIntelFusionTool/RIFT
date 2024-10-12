package dev.nohus.rift.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.about.GetPatronsUseCase.Patron
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.splash
import dev.nohus.rift.generated.resources.window_rift_64
import dev.nohus.rift.splash.SplashViewModel.UiState
import dev.nohus.rift.utils.viewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun SplashWindow(
    onCloseRequest: () -> Unit,
) {
    val viewModel: SplashViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    Window(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            width = 529.dp,
            height = 300.dp,
        ),
        title = "RIFT Intel Fusion Tool",
        icon = painterResource(Res.drawable.window_rift_64),
        decoration = WindowDecoration.Undecorated(),
        resizable = false,
        alwaysOnTop = true,
    ) {
        SplashWindowContent(
            state = state,
        )
    }
}

@Composable
private fun SplashWindowContent(state: UiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Image(
            painter = painterResource(Res.drawable.splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Column {
            Column(
                modifier = Modifier
                    .padding(top = Spacing.large)
                    .clip(RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(vertical = Spacing.large)
                    .padding(start = Spacing.large, end = 32.dp),
            ) {
                RiftAppName()
                Text(
                    text = state.version,
                    style = RiftTheme.typography.headlinePrimary.copy(color = Color.White),
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.patrons.isNotEmpty()) {
                Patrons(state.patrons)
            }
        }
    }
}

@Composable
fun RiftAppName() {
    Box {
        val duration = 1500
        val offsetAnimation = remember { Animatable(0f) }
        val alphaAnimation = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            offsetAnimation.animateTo(1f, animationSpec = tween(duration))
        }
        LaunchedEffect(Unit) {
            delay(duration / 2L)
            alphaAnimation.animateTo(1f, animationSpec = tween(duration / 2))
        }

        val texts = listOf("RIFT ", "Intel ", "Fusion ", "Tool")
        val textMeasurer = rememberTextMeasurer()
        val style = RiftTheme.typography.headlineHighlighted
            .copy(fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
        val widths = texts.map {
            textMeasurer.measure(it, style).size.width
        }
        val initialWidths = texts.map {
            textMeasurer.measure(it.take(1), style).size.width
        }
        val totalWidth = LocalDensity.current.run { widths.sum().toDp() }
        Box(Modifier.width(totalWidth)) {}

        texts.forEachIndexed { index, text ->
            val initialWidth = initialWidths[index]
            val finalOffset = widths.take(index).sum()
            val startingOffset = initialWidths.take(index).sum()
            val animatedOffset = (finalOffset - startingOffset) * offsetAnimation.value
            val offset = (startingOffset + animatedOffset).toInt()
            Text(
                text = text.take(1),
                style = style.copy(color = RiftTheme.colors.textSpecialHighlighted),
                modifier = Modifier.offset { IntOffset(offset, 0) },
            )
            Text(
                text = text.drop(1),
                style = style,
                modifier = Modifier
                    .offset { IntOffset(offset + initialWidth, 0) }
                    .graphicsLayer(alpha = alphaAnimation.value),
            )
        }
    }
}

@Composable
private fun Patrons(
    patrons: List<Patron>,
) {
    var isVisible by remember { mutableStateOf(false) }
    AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(delayMillis = 500))) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f))
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(start = Spacing.large)
                .padding(vertical = Spacing.medium),
        ) {
            Text(
                text = "PATRONS",
                style = RiftTheme.typography.headlineHighlighted.copy(fontWeight = FontWeight.Bold),
            )

            Layout(
                content = {
                    patrons.forEach {
                        Patron(it)
                    }
                },
            ) { measurables, constraints ->
                val placeables = measurables.map { measurable ->
                    measurable.measure(constraints)
                }
                val height = placeables.maxOf { it.height }
                layout(constraints.maxWidth, height) {
                    var xPosition = 0
                    placeables.forEach { placeable ->
                        if (xPosition + placeable.width <= constraints.maxWidth) {
                            placeable.placeRelative(xPosition, 0)
                            xPosition += placeable.width
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        isVisible = true
    }
}

@Composable
private fun Patron(patron: Patron) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = Spacing.medium),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
        ) {
            AsyncPlayerPortrait(
                characterId = patron.characterId,
                size = 32,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = patron.name,
            style = RiftTheme.typography.titlePrimary.copy(color = Color.White),
        )
    }
}
