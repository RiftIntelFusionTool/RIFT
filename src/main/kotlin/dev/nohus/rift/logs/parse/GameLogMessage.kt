package dev.nohus.rift.logs.parse

import java.time.Instant

data class GameLogMessage(
    val timestamp: Instant,
    val type: String,
    val message: String,
)
