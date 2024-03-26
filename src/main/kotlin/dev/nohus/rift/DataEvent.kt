package dev.nohus.rift

import java.util.UUID

data class DataEvent<T>(
    val value: T,
    val uuid: UUID = UUID.randomUUID(),
    var isConsumed: Boolean = false,
)

data class Event(
    val uuid: UUID = UUID.randomUUID(),
    var isConsumed: Boolean = false,
)

fun <T> DataEvent<T>?.get(): T? {
    if (this == null) return null
    return if (!isConsumed) {
        isConsumed = true
        return value
    } else {
        null
    }
}

fun Event?.get(): Boolean {
    if (this == null) return false
    return if (!isConsumed) {
        isConsumed = true
        return true
    } else {
        false
    }
}
