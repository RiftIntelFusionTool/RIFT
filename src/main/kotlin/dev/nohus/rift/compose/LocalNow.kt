package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun getNow(): Instant {
    val now: Instant by produceState(initialValue = Instant.now()) {
        while (true) {
            delay(333)
            value = Instant.now()
        }
    }
    return now
}

val LocalNow: ProvidableCompositionLocal<Instant> = staticCompositionLocalOf { Instant.now() }
