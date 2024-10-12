package dev.nohus.rift.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.onKeyPress(key: Key, action: () -> Unit): Modifier {
    return onKeyEvent {
        if (it.key == key && it.type == KeyEventType.KeyUp) {
            action()
            true
        } else {
            false
        }
    }
}
