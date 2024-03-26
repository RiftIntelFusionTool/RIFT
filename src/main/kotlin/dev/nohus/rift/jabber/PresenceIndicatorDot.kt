package dev.nohus.rift.jabber

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.jabber.client.RosterUsersController

@Composable
fun PresenceIndicatorDot(
    presence: RosterUsersController.RosterUserPresence,
    isSubscriptionPending: Boolean,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = when {
            isSubscriptionPending -> RiftTheme.colors.inactiveGray
            presence.mode == RosterUsersController.PresenceMode.FreeToChat -> RiftTheme.colors.onlineGreen
            presence.mode == RosterUsersController.PresenceMode.Available -> RiftTheme.colors.onlineGreen
            presence.mode == RosterUsersController.PresenceMode.Away -> RiftTheme.colors.awayYellow
            presence.mode == RosterUsersController.PresenceMode.ExtendedAway -> RiftTheme.colors.extendedAwayOrange
            presence.mode == RosterUsersController.PresenceMode.DoNotDisturb -> RiftTheme.colors.offlineRed
            else -> RiftTheme.colors.inactiveGray
        },
        animationSpec = tween(1000),
    )
    val blur by animateFloatAsState(
        targetValue = when {
            isSubscriptionPending -> 2f
            presence.mode == RosterUsersController.PresenceMode.FreeToChat -> 6f
            presence.mode == RosterUsersController.PresenceMode.Available -> 6f
            presence.mode == RosterUsersController.PresenceMode.Away -> 2f
            presence.mode == RosterUsersController.PresenceMode.ExtendedAway -> 2f
            presence.mode == RosterUsersController.PresenceMode.DoNotDisturb -> 2f
            else -> 2f
        },
        animationSpec = tween(1000),
    )
    val size = 12.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .border(4.dp, color, CircleShape),
        ) {}
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        ) {}
    }
}
