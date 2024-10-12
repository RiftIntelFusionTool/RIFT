package dev.nohus.rift.planetaryindustry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.OnVisibilityChange
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.bars_sort_ascending_16px
import dev.nohus.rift.generated.resources.checkmark_16px
import dev.nohus.rift.generated.resources.details_view_16px
import dev.nohus.rift.generated.resources.grid_view_16px
import dev.nohus.rift.generated.resources.list_view_16px
import dev.nohus.rift.generated.resources.pi_slotunlocked
import dev.nohus.rift.generated.resources.window_planets
import dev.nohus.rift.network.AsyncResource.Error
import dev.nohus.rift.network.AsyncResource.Loading
import dev.nohus.rift.network.AsyncResource.Ready
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.UiState
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.DetailsView
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.GridView
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.ListView
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.RowsView
import dev.nohus.rift.planetaryindustry.compose.ColonyOverview
import dev.nohus.rift.planetaryindustry.compose.ColonyPins
import dev.nohus.rift.planetaryindustry.compose.ColonyPlanetSnippet
import dev.nohus.rift.planetaryindustry.compose.ColonyTitle
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Idle
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NeedsAttention
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NotSetup
import dev.nohus.rift.settings.persistence.ColonySortingFilter
import dev.nohus.rift.settings.persistence.ColonyView
import dev.nohus.rift.utils.invertedPlural
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.time.Instant

@Composable
fun PlanetaryIndustryWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: PlanetaryIndustryViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Planetary Industry",
        icon = Res.drawable.window_planets,
        state = windowState,
        onCloseClick = onCloseRequest,
        withContentPadding = true,
    ) {
        PlanetaryIndustryWindowContent(
            state = state,
            onReloadClick = viewModel::onReloadClick,
            onRequestSimulation = viewModel::onRequestSimulation,
            onViewChange = viewModel::onViewChange,
            onDetailsClick = viewModel::onDetailsClick,
            onBackClick = viewModel::onBackClick,
            onSortingFilterChange = viewModel::onSortingFilterChange,
        )
        OnVisibilityChange(viewModel::onVisibilityChange)
    }
}

@Composable
private fun PlanetaryIndustryWindowContent(
    state: UiState,
    onReloadClick: () -> Unit,
    onRequestSimulation: () -> Unit,
    onViewChange: (ColonyView) -> Unit,
    onDetailsClick: (id: String) -> Unit,
    onBackClick: () -> Unit,
    onSortingFilterChange: (ColonySortingFilter) -> Unit,
) {
    when (val resource = state.colonies) {
        is Error -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            ) {
                Text(
                    text = "Could not load your colonies",
                    style = RiftTheme.typography.titlePrimary,
                    textAlign = TextAlign.Center,
                )
                RiftButton(
                    text = "Try again",
                    type = ButtonType.Primary,
                    onClick = onReloadClick,
                )
            }
        }

        Loading -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            ) {
                LoadingSpinner()
                Text(
                    text = "Loading colonies…",
                    style = RiftTheme.typography.titlePrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is Ready -> {
            val items = resource.value
            if (items.isNotEmpty()) {
                MainColoniesContent(
                    state = state,
                    items = resource.value,
                    onViewChange = onViewChange,
                    onBackClick = onBackClick,
                    onRequestSimulation = onRequestSimulation,
                    onDetailsClick = onDetailsClick,
                    onSortingFilterChange = onSortingFilterChange,
                )
            } else {
                EmptyState()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainColoniesContent(
    state: UiState,
    items: List<ColonyItem>,
    onViewChange: (ColonyView) -> Unit,
    onBackClick: () -> Unit,
    onRequestSimulation: () -> Unit,
    onDetailsClick: (id: String) -> Unit,
    onSortingFilterChange: (ColonySortingFilter) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val lazyGridRowsState = rememberLazyGridState()

    Column {
        AnimatedVisibility(
            visible = state.view == ListView || state.view == GridView || state.view == RowsView,
        ) {
            FiltersRow(
                state = state,
                onViewChange = onViewChange,
                onSortingFilterChange = onSortingFilterChange,
            )
        }

        // Shared element transition temporarily commented out due to crashes in LookaheadScope:
        // https://issuetracker.google.com/issues/368429360
        //
//        var sharedTransitionLayoutSize by remember { mutableStateOf(Offset.Zero) }
//        val overlayClip = remember(sharedTransitionLayoutSize) { getLayoutBoundsOverlayClip(sharedTransitionLayoutSize) }
//        SharedTransitionLayout(
//            modifier = Modifier.onSizeChanged {
//                sharedTransitionLayoutSize = Offset(it.width.toFloat(), it.height.toFloat())
//            },
//        ) {
        AnimatedContent(targetState = state.view) { view ->
            when (view) {
                is DetailsView -> {
                    ColonyDetails(
                        item = view.item,
                        now = Instant.now(),
//                            animatedVisibilityScope = this@AnimatedContent,
//                            sharedTransitionScope = this@SharedTransitionLayout,
                        onBackClick = onBackClick,
                        onRequestSimulation = onRequestSimulation,
                    )
                }

                ListView -> {
                    Column {
                        ScrollbarLazyColumn(
                            listState = lazyListState,
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            contentPadding = PaddingValues(top = Spacing.medium),
                        ) {
                            items(items, key = { it.colony.id }) { item ->
                                Column(
                                    modifier = Modifier.animateItem(),
                                ) {
                                    var isViewingFastForward by remember { mutableStateOf(false) }
                                    ColonyTitle(
                                        item = item,
                                        isExpanded = false,
                                        isViewingFastForward = isViewingFastForward,
                                        onViewFastForwardChange = { isViewingFastForward = it },
//                                            colonyIconModifier = Modifier
//                                                .sharedElement(
//                                                    rememberSharedContentState(item.colony.id),
//                                                    this@AnimatedContent,
//                                                    clipInOverlayDuringTransition = overlayClip,
//                                                ),
                                        onDetailsClick = { onDetailsClick(item.colony.id) },
                                    )
                                    if (isViewingFastForward) {
                                        ColonyOverview(
                                            colony = item.ffwdColony,
                                            now = item.ffwdColony.currentSimTime,
                                            isAdvancingTime = false,
                                            onRequestSimulation = {},
                                        )
                                    } else {
                                        ColonyOverview(
                                            colony = item.colony,
                                            now = Instant.now(),
                                            isAdvancingTime = true,
                                            onRequestSimulation = onRequestSimulation,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                GridView -> {
                    Column {
                        LazyVerticalGrid(
                            columns = GridCells.FixedSize(80.dp),
                            state = lazyGridState,
                            contentPadding = PaddingValues(top = Spacing.medium),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            items(items, key = { it.colony.id }) { item ->
                                ColonyPlanetSnippet(
                                    item = item,
//                                        colonyIconModifier = Modifier
//                                            .sharedElement(
//                                                rememberSharedContentState(item.colony.id),
//                                                this@AnimatedContent,
//                                                clipInOverlayDuringTransition = overlayClip,
//                                            ),
                                    isShowingCharacter = true,
                                    modifier = Modifier.animateItem(),
                                    onExpandClick = { onDetailsClick(item.colony.id) },
                                )
                            }
                        }
                    }
                }

                RowsView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        state = lazyGridRowsState,
                        contentPadding = PaddingValues(top = Spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        items.groupBy { it.colony.characterId }.forEach { (characterId, items) ->
                            item(key = characterId) {
                                RiftTooltipArea(
                                    text = items.first().characterName ?: "Loading…",
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .animateItem()
                                            .clip(CircleShape)
                                            .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
                                    ) {
                                        AsyncPlayerPortrait(
                                            characterId = characterId,
                                            size = 64,
                                            modifier = Modifier.size(64.dp),
                                        )
                                    }
                                }
                            }
                            items.forEach { item ->
                                item(key = item.colony.id) {
                                    ColonyPlanetSnippet(
                                        item = item,
                                        modifier = Modifier.animateItem(),
                                        isShowingCharacter = false,
                                        onExpandClick = { onDetailsClick(item.colony.id) },
                                    )
                                }
                            }
                            repeat(6 - items.size) {
                                item(key = "$characterId-empty-$it") {
                                    RiftTooltipArea(
                                        text = "Unestablished Colony",
                                        modifier = Modifier.animateItem(),
                                    ) {
                                        Image(
                                            painter = painterResource(Res.drawable.pi_slotunlocked),
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun getLayoutBoundsOverlayClip(sharedTransitionLayoutSize: Offset): SharedTransitionScope.OverlayClip {
    return object : SharedTransitionScope.OverlayClip {
        override fun getClipPath(
            state: SharedTransitionScope.SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Path {
            return Path().apply {
                addRect(Rect(Offset.Zero, sharedTransitionLayoutSize))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Text(
        text = "No established planetary colonies.",
        style = RiftTheme.typography.titlePrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ColonyDetails(
    item: ColonyItem,
    now: Instant,
//    sharedTransitionScope: SharedTransitionScope,
//    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onRequestSimulation: () -> Unit,
) {
    Column {
        var isViewingFastForward by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
//        with(sharedTransitionScope) {
        ColonyTitle(
            item = item,
            isExpanded = true,
            isViewingFastForward = isViewingFastForward,
            onViewFastForwardChange = { isViewingFastForward = it },
            scrollState = scrollState,
//                colonyIconModifier = Modifier
//                    .sharedElement(rememberSharedContentState(item.colony.id), animatedVisibilityScope),
            onDetailsClick = onBackClick,
        )
//        }
        ScrollbarColumn(
            scrollState = scrollState,
            contentPadding = PaddingValues(top = Spacing.medium),
        ) {
            AnimatedContent(isViewingFastForward) {
                if (it) {
                    ColonyPins(
                        colony = item.ffwdColony,
                        now = item.ffwdColony.currentSimTime,
                        isAdvancingTime = false,
                        onRequestSimulation = {},
                    )
                } else {
                    ColonyPins(
                        colony = item.colony,
                        now = now,
                        isAdvancingTime = true,
                        onRequestSimulation = onRequestSimulation,
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersRow(
    state: UiState,
    modifier: Modifier = Modifier,
    onViewChange: (ColonyView) -> Unit,
    onSortingFilterChange: (ColonySortingFilter) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.medium),
    ) {
        val viewItems = listOf<ContextMenuItem>(
            ContextMenuItem.TextItem(
                text = "Details view",
                iconResource = Res.drawable.list_view_16px,
                onClick = { onViewChange(ColonyView.List) },
            ),
            ContextMenuItem.TextItem(
                text = "List view",
                iconResource = Res.drawable.details_view_16px,
                onClick = { onViewChange(ColonyView.Rows) },
            ),
            ContextMenuItem.TextItem(
                text = "Grid view",
                iconResource = Res.drawable.grid_view_16px,
                onClick = { onViewChange(ColonyView.Grid) },
            ),
        )
        AnimatedContent(state.view) { view ->
            var isShown by remember { mutableStateOf(false) }
            RiftTooltipArea(
                text = "View mode",
            ) {
                val resource = when (view) {
                    is DetailsView -> Res.drawable.details_view_16px
                    GridView -> Res.drawable.grid_view_16px
                    ListView -> Res.drawable.list_view_16px
                    RowsView -> Res.drawable.details_view_16px
                }
                RiftImageButton(
                    resource = resource,
                    size = 16.dp,
                    onClick = { isShown = true },
                )
            }
            if (isShown) {
                val offset = with(LocalDensity.current) {
                    16.dp.toPx().toInt()
                }
                RiftContextMenuPopup(
                    items = viewItems,
                    offset = IntOffset(0, offset),
                    onDismissRequest = { isShown = false },
                )
            }
        }

        val sortingFilterItems = listOf<ContextMenuItem>(
            ContextMenuItem.TextItem(
                text = "By status",
                iconResource = Res.drawable.checkmark_16px.takeIf { ColonySortingFilter.Status == state.sortingFilter },
                onClick = { onSortingFilterChange(ColonySortingFilter.Status) },
            ),
            ContextMenuItem.TextItem(
                text = "By expiry time",
                iconResource = Res.drawable.checkmark_16px.takeIf { ColonySortingFilter.ExpiryTime == state.sortingFilter },
                onClick = { onSortingFilterChange(ColonySortingFilter.ExpiryTime) },
            ),
            ContextMenuItem.TextItem(
                text = "By character",
                iconResource = Res.drawable.checkmark_16px.takeIf { ColonySortingFilter.Character == state.sortingFilter },
                onClick = { onSortingFilterChange(ColonySortingFilter.Character) },
            ),
        )
        Box(contentAlignment = Alignment.BottomStart) {
            var isShown by remember { mutableStateOf(false) }
            RiftTooltipArea(
                text = "Sort By",
            ) {
                RiftImageButton(
                    resource = Res.drawable.bars_sort_ascending_16px,
                    size = 16.dp,
                    onClick = { isShown = true },
                )
            }
            if (isShown) {
                val offset = with(LocalDensity.current) {
                    16.dp.toPx().toInt()
                }
                RiftContextMenuPopup(
                    items = sortingFilterItems,
                    offset = IntOffset(0, offset),
                    onDismissRequest = { isShown = false },
                )
            }
        }

        Spacer(Modifier.weight(1f))
        state.colonies.success?.let { items ->
            val colonyCount = items.size
            val idleCount = items.count { it.colony.status is Idle }
            val needsAttentionCount = items.count { it.colony.status is NotSetup || it.colony.status is NeedsAttention }
            if (colonyCount > 0) {
                val text = buildString {
                    append("$colonyCount planet${colonyCount.plural}")
                    if (idleCount > 0) {
                        append(", $idleCount idle")
                    }
                    if (needsAttentionCount > 0) {
                        append(", $needsAttentionCount need${needsAttentionCount.invertedPlural} attention")
                    }
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.titlePrimary,
                )
            }
        }
    }
}
