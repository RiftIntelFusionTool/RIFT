package dev.nohus.rift.notifications

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.utils.Pos
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationEditWindow(
    position: Pos?,
    onCloseRequest: (editPos: Pos?, pos: Pos?) -> Unit,
) {
    val state = rememberWindowState(
        width = 300.dp,
        height = 120.dp,
        position = position?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.PlatformDefault,
    )
    Window(
        onCloseRequest = { onCloseRequest(null, null) },
        state = state,
        undecorated = true,
        alwaysOnTop = true,
        title = "Edit notification placement",
        icon = painterResource(Res.drawable.window_loudspeaker_icon),
    ) {
        window.minimumSize = Dimension(300, 120)

        var notificationOffset by remember { mutableStateOf(Pos(0, 0)) }

        WindowDraggableArea {
            val transition = rememberInfiniteTransition()
            val borderPhase by transition.animateFloat(
                0f,
                -20f,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(RiftTheme.colors.windowBackgroundSecondary)
                    .drawBehind {
                        drawRoundRect(
                            color = Color.Red,
                            style = Stroke(
                                width = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), borderPhase),
                            ),
                        )
                    }
                    .fillMaxSize()
                    .pointerHoverIcon(PointerIcon(Cursors.pointer))
                    .padding(Spacing.medium),
            ) {
                Text(
                    text = "Drag to choose position",
                    textAlign = TextAlign.Center,
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = "Click here when done",
                    textAlign = TextAlign.Center,
                    style = RiftTheme.typography.bodyLink,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                        .onClick {
                            val notificationEditPos = Pos(state.position.x.value.toInt(), state.position.y.value.toInt())
                            val notificationPos = Pos(notificationEditPos.x + notificationOffset.x, notificationEditPos.y + notificationOffset.y)
                            onCloseRequest(notificationEditPos, notificationPos)
                        }
                        .padding(bottom = Spacing.small),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(Color.Black)
                        .border(1.dp, RiftTheme.colors.borderGreyLight)
                        .fillMaxSize()
                        .onGloballyPositioned {
                            notificationOffset = it.positionInRoot().let { offset -> Pos(offset.x.toInt(), offset.y.toInt()) }
                        }
                        .padding(Spacing.medium),

                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                                append("Sample notification title\n")
                            }
                            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                                append("Sample notification contents")
                            }
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
