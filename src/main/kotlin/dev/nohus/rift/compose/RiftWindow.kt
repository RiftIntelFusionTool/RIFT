package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import dev.nohus.rift.Event
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.menu_close
import dev.nohus.rift.generated.resources.window_background_dots
import dev.nohus.rift.generated.resources.window_overlay_fullscreen_off_16px
import dev.nohus.rift.generated.resources.window_overlay_fullscreen_on_16px
import dev.nohus.rift.generated.resources.window_titlebar_close
import dev.nohus.rift.generated.resources.window_titlebar_minimize
import dev.nohus.rift.generated.resources.window_titlebar_tune
import dev.nohus.rift.get
import dev.nohus.rift.windowing.AlwaysOnTopController
import dev.nohus.rift.windowing.LocalRiftWindow
import dev.nohus.rift.windowing.LocalRiftWindowState
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

@Composable
fun RiftWindow(
    title: String,
    icon: DrawableResource,
    state: RiftWindowState,
    onTuneClick: (() -> Unit)? = null,
    tuneContextMenuItems: List<ContextMenuItem>? = null,
    onCloseClick: () -> Unit,
    titleBarStyle: TitleBarStyle = TitleBarStyle.Full,
    titleBarContent: @Composable ((height: Dp) -> Unit)? = null,
    withContentPadding: Boolean = true,
    isResizable: Boolean = true,
    content: @Composable WindowScope.() -> Unit,
) {
    val alwaysOnTopController: AlwaysOnTopController = remember { koin.get() }
    val isAlwaysOnTop by alwaysOnTopController.isAlwaysOnTop(state.window).collectAsState(false)
    Window(
        onCloseRequest = onCloseClick,
        state = state.windowState,
        visible = state.isVisible,
        title = title,
        icon = painterResource(icon),
        undecorated = true,
        resizable = isResizable,
        alwaysOnTop = isAlwaysOnTop,
    ) {
        MinimumSizeHandler(state)
        BringToFrontHandler(state.bringToFrontEvent)
        CompositionLocalProvider(
            LocalRiftWindow provides window,
            LocalRiftWindowState provides state,
        ) {
            RiftWindowContent(
                title = title,
                icon = icon,
                isAlwaysOnTop = isAlwaysOnTop,
                onTuneClick = onTuneClick,
                tuneContextMenuItems = tuneContextMenuItems,
                onAlwaysOnTopClick = if (state.window != null) {
                    { alwaysOnTopController.toggleAlwaysOnTop(state.window) }
                } else {
                    null
                },
                onMinimizeClick = { state.windowState.isMinimized = true },
                onCloseClick = onCloseClick,
                width = state.windowState.size.width,
                height = state.windowState.size.height,
                titleBarStyle = titleBarStyle,
                titleBarContent = titleBarContent,
                withContentPadding = withContentPadding,
                content = content,
            )
        }
    }
}

@Composable
private fun FrameWindowScope.MinimumSizeHandler(state: RiftWindowState) {
    var isSet by remember { mutableStateOf(false) }
    if (isSet) return
    LaunchedEffect(state.minimumSize, state.windowState.size) {
        val minimumWidth = state.minimumSize.first?.dp
            ?: state.windowState.size.width.takeIf { it != Dp.Unspecified } ?: return@LaunchedEffect
        val minimumHeight = state.minimumSize.second?.dp
            ?: state.windowState.size.height.takeIf { it != Dp.Unspecified } ?: return@LaunchedEffect
        val minimumSize = Dimension(minimumWidth.value.toInt(), minimumHeight.value.toInt())
        window.minimumSize = minimumSize
        isSet = true
    }
}

@Composable
private fun FrameWindowScope.BringToFrontHandler(event: Event?) {
    if (event.get()) {
        if (window.isMinimized) {
            window.isMinimized = false
        } else {
            window.isVisible = false
            window.isVisible = true
        }
    }
}

@Composable
fun WindowScope.RiftDialog(
    title: String,
    icon: DrawableResource,
    parentState: RiftWindowState,
    state: WindowState,
    onCloseClick: () -> Unit,
    content: @Composable WindowScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onCloseClick,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        CompositionLocalProvider(LocalRiftWindowState provides parentState) {
            RiftWindowContent(
                title = title,
                icon = icon,
                isAlwaysOnTop = false,
                onTuneClick = null,
                tuneContextMenuItems = null,
                onAlwaysOnTopClick = null,
                onMinimizeClick = { parentState.windowState.isMinimized = true },
                onCloseClick = onCloseClick,
                width = state.size.width,
                height = state.size.height,
                titleBarStyle = TitleBarStyle.Full,
                withContentPadding = true,
                content = content,
            )
        }
    }
}

enum class TitleBarStyle {
    Full, Small
}

@Composable
private fun WindowScope.RiftWindowContent(
    title: String,
    icon: DrawableResource,
    isAlwaysOnTop: Boolean,
    onTuneClick: (() -> Unit)?,
    tuneContextMenuItems: List<ContextMenuItem>?,
    onAlwaysOnTopClick: (() -> Unit)?,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
    width: Dp,
    height: Dp,
    titleBarStyle: TitleBarStyle,
    titleBarContent: @Composable ((height: Dp) -> Unit)? = null,
    withContentPadding: Boolean,
    content: @Composable WindowScope.() -> Unit,
) {
    val activeTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val backgroundColor by activeTransition.animateColor {
        if (it) RiftTheme.colors.windowBackgroundActive else RiftTheme.colors.windowBackground
    }

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .size(width, height)
            .pointerHoverIcon(PointerIcon(Cursors.pointer)),
    ) {
        BackgroundDots(activeTransition, width, height)
        WindowBorder(activeTransition, width, height)
        Column(
            modifier = Modifier.padding(1.dp),
        ) {
            TitleBar(
                style = titleBarStyle,
                title = title,
                icon = icon,
                titleBarContent = titleBarContent,
                isAlwaysOnTop = isAlwaysOnTop,
                onTuneClick = onTuneClick,
                tuneContextMenuItems = tuneContextMenuItems,
                onAlwaysOnTopClick = onAlwaysOnTopClick,
                onMinimizeClick = onMinimizeClick,
                onCloseClick = onCloseClick,
                width = width,
            )
            Box(
                modifier = Modifier
                    .modifyIf(withContentPadding) {
                        padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.large)
                    },
            ) {
                content()
            }
        }
    }
}

@Composable
private fun WindowBorder(
    activeTransition: Transition<Boolean>,
    width: Dp,
    height: Dp,
) {
    val transitionSpec = getActiveWindowTransitionSpec<Color>()
    val borderColor by activeTransition.animateColor(transitionSpec) {
        if (it) Color(0xFF1E2022) else Color(0xFF1F1F1F)
    }
    val activeBorderColor by activeTransition.animateColor(transitionSpec) {
        if (it) RiftTheme.colors.borderPrimary else Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(width, height)
            .border(1.dp, borderColor),
    )
    Box(
        modifier = Modifier
            .height(1.dp)
            .width(width)
            .background(activeBorderColor),
    )
    Box(
        modifier = Modifier
            .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal))
            .height(2.dp)
            .width(width)
            .background(activeBorderColor),
    )
}

@Composable
private fun WindowScope.TitleBar(
    style: TitleBarStyle,
    title: String,
    icon: DrawableResource,
    titleBarContent: @Composable ((height: Dp) -> Unit)? = null,
    width: Dp,
    isAlwaysOnTop: Boolean,
    onTuneClick: (() -> Unit)?,
    tuneContextMenuItems: List<ContextMenuItem>?,
    onAlwaysOnTopClick: (() -> Unit)?,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val alwaysOnTopItem = if (onAlwaysOnTopClick != null) {
        listOf(
            if (isAlwaysOnTop) {
                ContextMenuItem.TextItem(
                    "Disable always above",
                    Res.drawable.window_overlay_fullscreen_on_16px,
                    onClick = onAlwaysOnTopClick,
                )
            } else {
                ContextMenuItem.TextItem(
                    "Enable always above",
                    Res.drawable.window_overlay_fullscreen_off_16px,
                    onClick = onAlwaysOnTopClick,
                )
            },
        )
    } else {
        emptyList()
    }
    val contextMenuItems = alwaysOnTopItem + listOf(
        ContextMenuItem.TextItem("Minimize", onClick = onMinimizeClick),
        ContextMenuItem.DividerItem,
        ContextMenuItem.TextItem("Close", Res.drawable.menu_close, onClick = onCloseClick),
    )
    WindowDraggableArea {
        val horizontalPadding = when (style) {
            TitleBarStyle.Full -> Spacing.mediumLarge
            TitleBarStyle.Small -> Spacing.medium
        }
        val height = when (style) {
            TitleBarStyle.Full -> 48.dp
            TitleBarStyle.Small -> 32.dp
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .pointerHoverIcon(PointerIcon(Cursors.drag))
                .size(width, height),
        ) {
            RiftContextMenuArea(contextMenuItems) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = horizontalPadding),
                ) {
                    if (style == TitleBarStyle.Full) {
                        Image(
                            painter = painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = Spacing.medium)
                                .padding(vertical = Spacing.medium)
                                .size(32.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (titleBarContent != null) {
                    titleBarContent(height)
                } else {
                    RiftContextMenuArea(contextMenuItems, modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = when (style) {
                                TitleBarStyle.Full -> RiftTheme.typography.headlineHighlighted
                                TitleBarStyle.Small -> RiftTheme.typography.titleHighlighted
                            },
                        )
                    }
                }
            }
            RiftContextMenuArea(contextMenuItems) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(start = Spacing.medium)
                        .padding(vertical = Spacing.medium)
                        .padding(end = horizontalPadding),
                ) {
                    if (onTuneClick != null) {
                        RiftImageButton(Res.drawable.window_titlebar_tune, 16.dp, onTuneClick)
                    }
                    if (tuneContextMenuItems != null) {
                        RiftContextMenuArea(
                            items = tuneContextMenuItems,
                            acceptsLeftClick = true,
                            acceptsRightClick = false,
                        ) {
                            RiftImageButton(Res.drawable.window_titlebar_tune, 16.dp, {})
                        }
                    }
                    if (onAlwaysOnTopClick != null) {
                        if (isAlwaysOnTop) {
                            RiftImageButton(Res.drawable.window_overlay_fullscreen_on_16px, 16.dp, onAlwaysOnTopClick)
                        } else {
                            RiftImageButton(Res.drawable.window_overlay_fullscreen_off_16px, 16.dp, onAlwaysOnTopClick)
                        }
                    }
                    RiftImageButton(Res.drawable.window_titlebar_minimize, 16.dp, onMinimizeClick)
                    RiftImageButton(Res.drawable.window_titlebar_close, 16.dp, onCloseClick)
                }
            }
        }
    }
}

@Composable
private fun BackgroundDots(
    activeTransition: Transition<Boolean>,
    width: Dp,
    height: Dp,
) {
    val bitmap = imageResource(Res.drawable.window_background_dots)
    val brush = remember(bitmap) { ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated)) }
    val transitionSpec = getActiveWindowTransitionSpec<Float>()
    val alpha by activeTransition.animateFloat(transitionSpec) { if (it) 1f else 0f }
    Box(Modifier.alpha(alpha).size(width, height).background(brush))
}

fun <T> getActiveWindowTransitionSpec(): @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            false isTransitioningTo true -> spring(stiffness = Spring.StiffnessMedium)
            else -> spring(stiffness = Spring.StiffnessVeryLow)
        }
    }
}
