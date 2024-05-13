package dev.nohus.rift.logs

import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime

data class GameLogFile(
    val file: Path,
    val dateTime: LocalDateTime,
    val characterId: String,
    val lastModified: Instant,
)
