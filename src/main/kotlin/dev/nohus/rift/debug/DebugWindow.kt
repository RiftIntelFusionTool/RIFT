package dev.nohus.rift.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.unit.dp
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.VerticalDivider
import dev.nohus.rift.compose.animateBackgroundHover
import dev.nohus.rift.compose.animateWindowBackgroundSecondaryHover
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.debug.DebugViewModel.UiState
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_log
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DebugWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: DebugViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Debug",
        icon = Res.drawable.window_log,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        DebugWindowContent(
            state = state,
        )
    }
}

@Composable
private fun DebugWindowContent(
    state: UiState,
) {
    Column {
        var minLevel by remember { mutableStateOf(Level.ALL) }
        var isAutoScrolling by remember { mutableStateOf(true) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(bottom = Spacing.medium),
        ) {
            Text(
                text = "Level:",
                style = RiftTheme.typography.bodyPrimary,
            )
            RiftRadioButtonWithLabel(
                label = "All",
                isChecked = minLevel == Level.ALL,
                onChecked = { minLevel = Level.ALL },
            )
            RiftRadioButtonWithLabel(
                label = "Info",
                isChecked = minLevel == Level.INFO,
                onChecked = { minLevel = Level.INFO },
            )
            RiftRadioButtonWithLabel(
                label = "Warn",
                isChecked = minLevel == Level.WARN,
                onChecked = { minLevel = Level.WARN },
            )
            RiftRadioButtonWithLabel(
                label = "Error",
                isChecked = minLevel == Level.ERROR,
                onChecked = { minLevel = Level.ERROR },
            )
            RiftCheckboxWithLabel(
                label = "Autoscroll",
                isChecked = isAutoScrolling,
                onCheckedChange = { isAutoScrolling = it },
            )
        }
        LogsView(state, minLevel, isAutoScrolling)
    }
}

@Composable
private fun LogsView(
    state: UiState,
    minLevel: Level,
    isAutoScrolling: Boolean,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.events) {
        if (isAutoScrolling) {
            state.events.lastIndex.takeIf { it > -1 }?.let {
                listState.scrollToItem(it)
            }
        }
    }

    SelectionContainer {
        ScrollbarLazyColumn(
            listState = listState,
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            val filtered = state.events.filter { it.level.isGreaterOrEqual(minLevel) }
            items(filtered) { event ->
                val pointerState = remember { PointerInteractionStateHolder() }
                val background by pointerState.animateBackgroundHover()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier
                        .pointerInteraction(pointerState)
                        .background(background)
                        .fillMaxWidth(),
                ) {
                    EventMetadata(state, event, pointerState)
                    val style = when (event.level) {
                        Level.TRACE -> RiftTheme.typography.bodySecondary
                        Level.DEBUG -> RiftTheme.typography.bodySecondary
                        Level.WARN -> RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.awayYellow)
                        Level.ERROR -> RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.offlineRed)
                        else -> RiftTheme.typography.bodyPrimary
                    }
                    Text(
                        text = event.message,
                        style = style,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventMetadata(
    state: UiState,
    event: ILoggingEvent,
    pointerState: PointerInteractionStateHolder,
) {
    val background by pointerState.animateWindowBackgroundSecondaryHover()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .heightIn(min = 24.dp)
            .border(1.dp, RiftTheme.colors.borderGrey)
            .background(background),
    ) {
        val time = ZonedDateTime.ofInstant(event.instant, state.displayTimezone).toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formattedTime = formatter.format(time)
        Text(
            text = formattedTime,
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
        VerticalDivider(color = RiftTheme.colors.borderGrey)
        Text(
            text = event.level.toString(),
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
        VerticalDivider(color = RiftTheme.colors.borderGrey)
        Text(
            text = event.loggerName.substringAfterLast("."),
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
    }
}
