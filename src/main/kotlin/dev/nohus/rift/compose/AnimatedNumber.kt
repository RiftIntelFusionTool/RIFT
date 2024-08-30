package dev.nohus.rift.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import java.text.NumberFormat
import java.util.UUID

private enum class AnimationDirection {
    Up, Down
}

private data class TickerCharacter(
    val new: String,
    val animationDirection: AnimationDirection,
    val uuid: UUID,
)

private val numberFormat = NumberFormat.getIntegerInstance()

@Composable
fun TickerInterpolatedNumber(
    number: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val interpolatedNumber by animateIntAsState(targetValue = number, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val newText = numberFormat.format(interpolatedNumber)

    Row(
        modifier = modifier,
    ) {
        newText.forEach { character ->
            Text(
                text = character.toString(),
                style = style,
            )
        }
    }
}

@Composable
fun TickerIndividualDigits(
    number: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val newText = numberFormat.format(number)
    Row(
        modifier = modifier,
    ) {
        newText.toCharArray().forEach { character ->
            val spring = spring(visibilityThreshold = IntOffset.VisibilityThreshold, stiffness = Spring.StiffnessLow)
            fun AnimatedContentTransitionScope<Char>.getTransitionSpec(oldCharacter: Char, newCharacter: Char): ContentTransform {
                val direction = if (oldCharacter.isDigit() && newCharacter.isDigit() && oldCharacter.digitToInt() > newCharacter.digitToInt()) {
                    AnimatedContentTransitionScope.SlideDirection.Up
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Down
                }
                val transform = (slideIntoContainer(direction, spring) + fadeIn()).togetherWith(
                    slideOutOfContainer(direction, spring) + fadeOut(),
                )
                return transform.using(SizeTransform(clip = false))
            }

            val target = if (character.isDigit()) {
                val animatedDigit by animateIntAsState(targetValue = character.digitToInt())
                animatedDigit.toString()[0]
            } else {
                character
            }

            AnimatedContent(
                targetState = target,
                transitionSpec = { getTransitionSpec(initialState, targetState) },
                label = "Ticker",
            ) {
                Text(
                    text = it.toString(),
                    style = style,
                )
            }
        }
    }
}

@Composable
fun TickerUpdatedChunk(
    number: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    var currentNumber by remember { mutableStateOf(number) }
    val animationDirection = if (number > currentNumber) AnimationDirection.Down else AnimationDirection.Up
    currentNumber = number

    var currentText by remember { mutableStateOf(numberFormat.format(number)) }
    val newText = numberFormat.format(number)
    val commonPrefixLength = if (newText.length == currentText.length) {
        newText.commonPrefixWith(currentText).length
    } else {
        0
    }
    currentText = newText

    var currentTicker by remember {
        mutableStateOf(
            currentText.chunked(1).map { character ->
                TickerCharacter(
                    new = character,
                    animationDirection = AnimationDirection.Down,
                    uuid = UUID.randomUUID(),
                )
            },
        )
    }

    val newTicker = newText.chunked(1).mapIndexed { index, character ->
        if (index < commonPrefixLength) {
            currentTicker[index]
        } else {
            TickerCharacter(
                new = character,
                animationDirection = animationDirection,
                uuid = UUID.randomUUID(),
            )
        }
    }
    currentTicker = newTicker

    Row(
        modifier = modifier,
    ) {
        currentTicker.forEach {
            TickerUpdatedChunkCharacter(
                character = it,
                style = style,
            )
        }
    }
}

@Composable
private fun TickerUpdatedChunkCharacter(
    character: TickerCharacter,
    style: TextStyle,
) {
    val spring = spring(visibilityThreshold = IntOffset.VisibilityThreshold, stiffness = Spring.StiffnessLow)
    fun AnimatedContentTransitionScope<TickerCharacter>.getTransitionSpec(character: TickerCharacter): ContentTransform {
        val direction = when (character.animationDirection) {
            AnimationDirection.Up -> AnimatedContentTransitionScope.SlideDirection.Up
            AnimationDirection.Down -> AnimatedContentTransitionScope.SlideDirection.Down
        }
        val transform = (slideIntoContainer(direction, spring) + fadeIn()).togetherWith(slideOutOfContainer(direction, spring) + fadeOut())
        return transform.using(SizeTransform(clip = false))
    }
    AnimatedContent(
        targetState = character,
        transitionSpec = { getTransitionSpec(targetState) },
        label = "Ticker",
    ) {
        Text(
            text = it.new,
            style = style,
        )
    }
}
