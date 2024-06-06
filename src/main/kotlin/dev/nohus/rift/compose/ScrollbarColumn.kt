package dev.nohus.rift.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ScrollbarColumn(
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    scrollbarModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isScrollbarConditional: Boolean = false,
    hasScrollbarBackground: Boolean = false,
    isFillWidth: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        val density = LocalDensity.current
        var scrollbarHeight by remember { mutableStateOf(0.dp) }
        Column(
            verticalArrangement = verticalArrangement,
            modifier = Modifier
                .modifyIf(isFillWidth) { weight(1f) }
                .verticalScroll(scrollState)
                .onSizeChanged {
                    with(density) {
                        scrollbarHeight = it.height.toDp()
                    }
                }
                .padding(contentPadding),
        ) {
            content()
        }
        if (!isScrollbarConditional || (scrollState.canScrollBackward || scrollState.canScrollForward)) {
            RiftVerticalScrollbar(
                hasBackground = hasScrollbarBackground,
                scrollState = scrollState,
                modifier = scrollbarModifier.height(scrollbarHeight),
            )
        }
    }
}

@Composable
fun ScrollbarLazyColumn(
    listState: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    scrollbarModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        val density = LocalDensity.current
        var scrollbarHeight by remember { mutableStateOf(0.dp) }
        LazyColumn(
            state = listState,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            modifier = Modifier
                .weight(1f)
                .onSizeChanged {
                    with(density) {
                        scrollbarHeight = it.height.toDp()
                    }
                },
            content = content,
        )
        RiftVerticalScrollbar(
            listState = listState,
            modifier = scrollbarModifier.height(scrollbarHeight),
        )
    }
}
