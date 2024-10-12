package dev.nohus.rift.standings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun Standing.getColor(): Color? {
    return when (this) {
        Standing.Terrible -> RiftTheme.colors.standingTerrible
        Standing.Bad -> RiftTheme.colors.standingBad
        Standing.Neutral -> null
        Standing.Good -> RiftTheme.colors.standingGood
        Standing.Excellent -> RiftTheme.colors.standingExcellent
    }
}

val Standing.isFriendly: Boolean get() {
    return when (this) {
        Standing.Terrible -> false
        Standing.Bad -> false
        Standing.Neutral -> false
        Standing.Good -> true
        Standing.Excellent -> true
    }
}
