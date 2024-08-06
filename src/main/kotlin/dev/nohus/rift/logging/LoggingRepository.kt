package dev.nohus.rift.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LoggingRepository {

    private val _state = MutableStateFlow<List<ILoggingEvent>>(emptyList())
    val state = _state.asStateFlow()

    fun append(event: ILoggingEvent) {
        _state.value += event
    }
}
