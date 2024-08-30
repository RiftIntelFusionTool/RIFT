package dev.nohus.rift.intel.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.IntelSystem
import dev.nohus.rift.compose.IntelTimer
import dev.nohus.rift.compose.PointerInteractionState
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.SystemEntities
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.getStandardTransitionSpec
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_satellite
import dev.nohus.rift.intel.feed.IntelFeedViewModel.UiState
import dev.nohus.rift.map.groupIntelByTime
import dev.nohus.rift.settings.persistence.DistanceFilter
import dev.nohus.rift.settings.persistence.EntityFilter
import dev.nohus.rift.settings.persistence.LocationFilter
import dev.nohus.rift.settings.persistence.SortingFilter
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun IntelFeedWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
    onTuneClick: () -> Unit,
) {
    val viewModel: IntelFeedViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Intel Feed",
        icon = Res.drawable.window_satellite,
        state = windowState,
        onTuneClick = onTuneClick,
        onCloseClick = onCloseRequest,
        titleBarStyle = if (state.settings.isUsingCompactMode) TitleBarStyle.Small else TitleBarStyle.Full,
        withContentPadding = false,
    ) {
        IntelFeedWindowContent(
            state = state,
            onLocationFilterSelect = viewModel::onLocationFilterSelect,
            onDistanceFilterSelect = viewModel::onDistanceFilterSelect,
            onEntityFilterSelect = viewModel::onEntityFilterSelect,
            onSortingFilterSelect = viewModel::onSortingFilterSelect,
            onSearchChange = viewModel::onSearchChange,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntelFeedWindowContent(
    state: UiState,
    onLocationFilterSelect: (LocationFilter) -> Unit,
    onDistanceFilterSelect: (DistanceFilter) -> Unit,
    onEntityFilterSelect: (EntityFilter) -> Unit,
    onSortingFilterSelect: (SortingFilter) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    val outerPadding = if (state.settings.isUsingCompactMode) Spacing.medium else Spacing.large
    Column {
        FiltersRow(
            padding = outerPadding,
            state = state,
            onLocationFilterSelect = onLocationFilterSelect,
            onDistanceFilterSelect = onDistanceFilterSelect,
            onEntityFilterSelect = onEntityFilterSelect,
            onSortingFilterSelect = onSortingFilterSelect,
            onSearchChange = onSearchChange,
        )

        val items = state.intel

        val listState = rememberLazyListState()
        LaunchedEffect(items) {
            if (listState.firstVisibleItemIndex <= 1) {
                listState.animateScrollToItem(0)
            }
        }
        LaunchedEffect(state.settings.sortingFilter) {
            listState.animateScrollToItem(0)
        }

        if (items.isNotEmpty()) {
            var expandedSystem: String? by remember { mutableStateOf(null) }
            ScrollbarLazyColumn(
                listState = listState,
                modifier = Modifier.padding(start = outerPadding, bottom = outerPadding),
                scrollbarModifier = Modifier.padding(end = outerPadding / 2),
            ) {
                items(items, key = { it.first }) { (system, intel) ->
                    AnimatedContent(
                        targetState = system == expandedSystem,
                        transitionSpec = {
                            (
                                fadeIn(animationSpec = tween(110, delayMillis = 90)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = tween(110, delayMillis = 90))
                                )
                                .togetherWith(fadeOut(animationSpec = tween(90)))
                        },
                    ) { isExpanded ->
                        ItemBox(
                            isExpanded = isExpanded,
                            isCompact = state.settings.isUsingCompactMode,
                            onClick = { expandedSystem = if (isExpanded) null else system },
                            modifier = Modifier.animateItem(),
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (isExpanded) {
                                    IntelSystem(
                                        system = system,
                                        rowHeight = state.settings.rowHeight,
                                        isShowingSystemDistance = state.settings.isShowingSystemDistance,
                                        isUsingJumpBridges = state.settings.isUsingJumpBridgesForDistance,
                                        background = RiftTheme.colors.windowBackgroundSecondary,
                                    )
                                }
                                val groups = groupIntelByTime(intel)
                                for ((index, group) in groups.entries.sortedByDescending { it.key }.withIndex()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        if (index == 0 && !isExpanded) {
                                            IntelSystem(
                                                system = system,
                                                rowHeight = state.settings.rowHeight,
                                                isShowingSystemDistance = state.settings.isShowingSystemDistance,
                                                isUsingJumpBridges = state.settings.isUsingJumpBridgesForDistance,
                                                background = RiftTheme.colors.windowBackgroundSecondary,
                                            )
                                        }
                                        IntelTimer(
                                            timestamp = group.key,
                                            style = RiftTheme.typography.captionBoldPrimary,
                                            rowHeight = state.settings.rowHeight,
                                            modifier = Modifier.padding(Spacing.small),
                                        )
                                        SystemEntities(
                                            entities = group.value,
                                            system = system,
                                            rowHeight = state.settings.rowHeight,
                                            isHorizontal = true,
                                            isGroupingCharacters = !isExpanded,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val text = if (state.settings.locationFilters.isEmpty()) {
                "All locations have been filtered out.\nUpdate your locations filter."
            } else if (state.settings.entityFilters.isEmpty()) {
                "All types have been filtered out.\nUpdate your types filter."
            } else if (state.totalIntelSystems > 0) {
                "All intel has been filtered out.\nUpdate your filters."
            } else {
                "No intel available.\nWaiting for reports."
            }
            Text(
                text = text,
                style = RiftTheme.typography.titlePrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemBox(
    isExpanded: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Box(
        modifier = modifier
            .onClick { onClick() }
            .pointerInteraction(pointerInteractionStateHolder),
    ) {
        val colorTransitionSpec = getStandardTransitionSpec<Color>()
        val dpTransitionSpec = getStandardTransitionSpec<Dp>()
        val transition = updateTransition(pointerInteractionStateHolder.current)
        val background by transition.animateColor(colorTransitionSpec) {
            when (it) {
                PointerInteractionState.Normal -> RiftTheme.colors.windowBackgroundActive
                PointerInteractionState.Hover -> RiftTheme.colors.backgroundPrimaryDark
                PointerInteractionState.Press -> RiftTheme.colors.backgroundPrimary
            }
        }
        val border by transition.animateColor(colorTransitionSpec) {
            if (isExpanded) {
                RiftTheme.colors.borderPrimary
            } else {
                when (it) {
                    PointerInteractionState.Normal -> RiftTheme.colors.windowBackgroundActive
                    PointerInteractionState.Hover -> RiftTheme.colors.borderPrimary
                    PointerInteractionState.Press -> RiftTheme.colors.borderPrimary
                }
            }
        }
        val outerPadding = if (isCompact) Spacing.small else Spacing.medium
        val padding by transition.animateDp(dpTransitionSpec) {
            if (isExpanded) {
                outerPadding
            } else {
                when (it) {
                    PointerInteractionState.Normal -> Spacing.verySmall
                    PointerInteractionState.Hover -> outerPadding
                    PointerInteractionState.Press -> outerPadding
                }
            }
        }

        Surface(
            color = background,
            shape = CutCornerShape(bottomEnd = 15.dp),
            border = BorderStroke(1.dp, border),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = padding, end = (outerPadding * 2) - padding)
                    .padding(vertical = outerPadding),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun FiltersRow(
    padding: Dp,
    state: UiState,
    onLocationFilterSelect: (LocationFilter) -> Unit,
    onDistanceFilterSelect: (DistanceFilter) -> Unit,
    onEntityFilterSelect: (EntityFilter) -> Unit,
    onSortingFilterSelect: (SortingFilter) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    val settings = state.settings
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = padding, end = padding, bottom = padding / 2),
    ) {
        val height = if (settings.isUsingCompactMode) 24.dp else 32.dp
        Text(
            text = "Filters:",
            style = RiftTheme.typography.bodyPrimary,
        )

        val locationFilterItems = listOf<ContextMenuItem>(
            ContextMenuItem.CheckboxItem(
                text = "Known space",
                onClick = { onLocationFilterSelect(LocationFilter.KnownSpace) },
                isSelected = LocationFilter.KnownSpace in settings.locationFilters,
            ),
            ContextMenuItem.CheckboxItem(
                text = "Wormhole space",
                onClick = { onLocationFilterSelect(LocationFilter.WormholeSpace) },
                isSelected = LocationFilter.WormholeSpace in settings.locationFilters,
            ),
            ContextMenuItem.CheckboxItem(
                text = "Abyssal space",
                onClick = { onLocationFilterSelect(LocationFilter.AbyssalSpace) },
                isSelected = LocationFilter.AbyssalSpace in settings.locationFilters,
            ),
            ContextMenuItem.CheckboxItem(
                text = "Opened map region",
                onClick = { onLocationFilterSelect(LocationFilter.CurrentMapRegion) },
                isSelected = LocationFilter.CurrentMapRegion in settings.locationFilters,
            ),
        )
        Box(contentAlignment = Alignment.BottomStart) {
            var isShown by remember { mutableStateOf(false) }
            RiftButton(
                text = "Location",
                onClick = { isShown = true },
                modifier = Modifier.height(height),
            )
            if (isShown) {
                val offset = with(LocalDensity.current) {
                    32.dp.toPx().toInt()
                }
                RiftContextMenuPopup(
                    items = locationFilterItems,
                    offset = IntOffset(0, offset),
                    onDismissRequest = { isShown = false },
                )
            }
        }

        val distanceFilterItems = listOf<ContextMenuItem>(
            ContextMenuItem.RadioItem(
                text = "All",
                onClick = { onDistanceFilterSelect(DistanceFilter.All) },
                isSelected = DistanceFilter.All == settings.distanceFilter,
            ),
            ContextMenuItem.RadioItem(
                text = "Within region",
                onClick = { onDistanceFilterSelect(DistanceFilter.CharacterLocationRegions) },
                isSelected = DistanceFilter.CharacterLocationRegions == settings.distanceFilter,
            ),
            ContextMenuItem.RadioItem(
                text = "Within 9 jumps",
                onClick = { onDistanceFilterSelect(DistanceFilter.WithinDistance(9)) },
                isSelected = DistanceFilter.WithinDistance(9) == settings.distanceFilter,
            ),
            ContextMenuItem.RadioItem(
                text = "Within 7 jumps",
                onClick = { onDistanceFilterSelect(DistanceFilter.WithinDistance(7)) },
                isSelected = DistanceFilter.WithinDistance(7) == settings.distanceFilter,
            ),
            ContextMenuItem.RadioItem(
                text = "Within 5 jumps",
                onClick = { onDistanceFilterSelect(DistanceFilter.WithinDistance(5)) },
                isSelected = DistanceFilter.WithinDistance(5) == settings.distanceFilter,
            ),
            ContextMenuItem.RadioItem(
                text = "Within 3 jumps",
                onClick = { onDistanceFilterSelect(DistanceFilter.WithinDistance(3)) },
                isSelected = DistanceFilter.WithinDistance(3) == settings.distanceFilter,
            ),
        )
        Box(contentAlignment = Alignment.BottomStart) {
            var isShown by remember { mutableStateOf(false) }
            RiftButton(
                text = "Distance",
                onClick = { isShown = true },
                modifier = Modifier.height(height),
            )
            if (isShown) {
                val offset = with(LocalDensity.current) {
                    height.toPx().toInt()
                }
                RiftContextMenuPopup(
                    items = distanceFilterItems,
                    offset = IntOffset(0, offset),
                    onDismissRequest = { isShown = false },
                )
            }
        }

        val entityFilterItems = listOf<ContextMenuItem>(
            ContextMenuItem.CheckboxItem(
                text = "Killmails",
                onClick = { onEntityFilterSelect(EntityFilter.Killmails) },
                isSelected = EntityFilter.Killmails in settings.entityFilters,
            ),
            ContextMenuItem.CheckboxItem(
                text = "Characters & ships",
                onClick = { onEntityFilterSelect(EntityFilter.Characters) },
                isSelected = EntityFilter.Characters in settings.entityFilters,
            ),
            ContextMenuItem.CheckboxItem(
                text = "Other intel",
                onClick = { onEntityFilterSelect(EntityFilter.Other) },
                isSelected = EntityFilter.Other in settings.entityFilters,
            ),
        )
        Box(contentAlignment = Alignment.BottomStart) {
            var isShown by remember { mutableStateOf(false) }
            RiftButton(
                text = "Types",
                onClick = { isShown = true },
                modifier = Modifier.height(height),
            )
            if (isShown) {
                val offset = with(LocalDensity.current) {
                    height.toPx().toInt()
                }
                RiftContextMenuPopup(
                    items = entityFilterItems,
                    offset = IntOffset(0, offset),
                    onDismissRequest = { isShown = false },
                )
            }
        }

        val sortingFilterItems = listOf<ContextMenuItem>(
            ContextMenuItem.RadioItem(
                text = "By time",
                onClick = { onSortingFilterSelect(SortingFilter.Time) },
                isSelected = SortingFilter.Time == settings.sortingFilter,
            ),
            ContextMenuItem.RadioItem(
                text = "By distance",
                onClick = { onSortingFilterSelect(SortingFilter.Distance) },
                isSelected = SortingFilter.Distance == settings.sortingFilter,
            ),
        )
        Box(contentAlignment = Alignment.BottomStart) {
            var isShown by remember { mutableStateOf(false) }
            RiftButton(
                text = "Sorting",
                onClick = { isShown = true },
                modifier = Modifier.height(height),
            )
            if (isShown) {
                val offset = with(LocalDensity.current) {
                    height.toPx().toInt()
                }
                RiftContextMenuPopup(
                    items = sortingFilterItems,
                    offset = IntOffset(0, offset),
                    onDismissRequest = { isShown = false },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        RiftSearchField(
            search = state.search,
            isCompact = state.settings.isUsingCompactMode,
            onSearchChange = onSearchChange,
        )
    }
}
