package dev.nohus.rift.intel.reports.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.ZoneId

data class IntelReportsSettings(
    val displayTimezone: ZoneId,
    val isUsingCompactMode: Boolean,
    val isShowingReporter: Boolean,
    val isShowingChannel: Boolean,
    val isShowingRegion: Boolean,
    val isShowingSystemDistance: Boolean,
    val isUsingJumpBridgesForDistance: Boolean,
) {
    val rowHeight: Dp get() = if (isUsingCompactMode) 24.dp else 32.dp
}
