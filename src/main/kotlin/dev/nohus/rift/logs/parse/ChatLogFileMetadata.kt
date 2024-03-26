package dev.nohus.rift.logs.parse

data class ChatLogFileMetadata(
    val channelId: String,
    val channelName: String,
    val listener: String, // Character name
    val characterId: Int,
    val sessionStarted: String,
    val groupName: String?, // Name of corporation or alliance if this is a corporation or alliance channel
    val motd: String?, // MOTD of the channel if it has one
)
