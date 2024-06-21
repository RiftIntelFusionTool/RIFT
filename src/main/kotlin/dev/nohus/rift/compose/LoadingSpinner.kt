package dev.nohus.rift.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.loading_bar
import dev.nohus.rift.generated.resources.loading_track
import dev.nohus.rift.generated.resources.loading_track_overlay
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(Res.drawable.loading_track),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        val transition = rememberInfiniteTransition()
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        )
        Image(
            painter = painterResource(Res.drawable.loading_bar),
            contentDescription = null,
            modifier = Modifier.size(72.dp).rotate(rotation - 90),
        )
        Image(
            painter = painterResource(Res.drawable.loading_track_overlay),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
    }
}
