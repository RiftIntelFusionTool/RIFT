package dev.nohus.rift.logs.parse

import java.time.Instant

data class ChatMessage(
    val timestamp: Instant,
    val author: String,
    val message: String,
)
