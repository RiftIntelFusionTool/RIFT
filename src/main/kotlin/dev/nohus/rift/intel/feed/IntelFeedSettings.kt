package dev.nohus.rift.intel.feed

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.settings.persistence.DistanceFilter
import dev.nohus.rift.settings.persistence.EntityFilter
import dev.nohus.rift.settings.persistence.LocationFilter
import dev.nohus.rift.settings.persistence.SortingFilter

data class IntelFeedSettings(
    val locationFilters: List<LocationFilter>,
    val distanceFilter: DistanceFilter,
    val entityFilters: List<EntityFilter>,
    val sortingFilter: SortingFilter,
    val isUsingCompactMode: Boolean,
    val isShowingSystemDistance: Boolean,
    val isUsingJumpBridgesForDistance: Boolean,
) {
    val rowHeight: Dp get() = if (isUsingCompactMode) 24.dp else 32.dp
}
