package dev.nohus.rift.compose

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> InfiniteScrollingCarousel(
    items: List<T>,
    delay: Int = 75,
    modifier: Modifier = Modifier,
    content: @Composable (item: T) -> Unit,
) {
    val listState = rememberLazyListState()
    val delay = (delay / LocalDensity.current.density).roundToLong()
    var isScrolling by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            if (isScrolling) listState.scrollBy(1f)
            delay(delay)
        }
    }
    LazyRow(
        state = listState,
        userScrollEnabled = false,
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { isScrolling = false }
            .onPointerEvent(PointerEventType.Exit) { isScrolling = true },
    ) {
        items(Int.MAX_VALUE) { index ->
            val item = items[index % items.size]
            content(item)
        }
    }
}
