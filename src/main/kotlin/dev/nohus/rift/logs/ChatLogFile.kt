package dev.nohus.rift.logs

import java.io.File
import java.time.Instant
import java.time.LocalDateTime

data class ChatLogFile(
    val file: File,
    val channelName: String,
    val dateTime: LocalDateTime,
    val characterId: String,
    val lastModified: Instant,
)
