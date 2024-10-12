package dev.nohus.rift.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.status_ok
import dev.nohus.rift.generated.resources.status_warning
import org.jetbrains.compose.resources.painterResource

@Composable
fun RequirementIcon(
    isFulfilled: Boolean,
    fulfilledTooltip: String,
    notFulfilledTooltip: String,
    modifier: Modifier = Modifier,
) {
    val tooltip = if (isFulfilled) fulfilledTooltip else notFulfilledTooltip
    RiftTooltipArea(tooltip, modifier) {
        val icon = if (isFulfilled) Res.drawable.status_ok else Res.drawable.status_warning
        AnimatedContent(icon) {
            Image(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
