package dev.nohus.rift.logs.parse

data class ChannelChatMessage(
    val chatMessage: ChatMessage,
    val metadata: ChatLogFileMetadata,
)
