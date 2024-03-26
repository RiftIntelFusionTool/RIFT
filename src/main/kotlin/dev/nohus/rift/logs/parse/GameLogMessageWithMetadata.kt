package dev.nohus.rift.logs.parse

data class GameLogMessageWithMetadata(
    val message: GameLogMessage,
    val metadata: GameLogFileMetadata,
)
