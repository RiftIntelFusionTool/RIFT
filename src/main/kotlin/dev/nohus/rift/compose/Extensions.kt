package dev.nohus.rift.compose

import androidx.compose.ui.Modifier

fun Modifier.modifyIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

fun <T> Modifier.modifyIfNotNull(element: T?, modifier: Modifier.(T) -> Modifier): Modifier {
    return if (element != null) {
        then(modifier(Modifier, element))
    } else {
        this
    }
}
