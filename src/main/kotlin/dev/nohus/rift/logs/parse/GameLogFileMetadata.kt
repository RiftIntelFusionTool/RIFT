package dev.nohus.rift.logs.parse

data class GameLogFileMetadata(
    val listener: String, // Character name
    val characterId: Int,
    val sessionStarted: String,
)
