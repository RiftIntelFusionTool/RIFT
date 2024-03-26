package dev.nohus.rift.intel

import dev.nohus.rift.logs.parse.ChatLogFileMetadata
import dev.nohus.rift.logs.parse.ChatMessage
import dev.nohus.rift.logs.parse.ChatMessageParser.Token

data class ParsedChannelChatMessage(
    val chatMessage: ChatMessage,
    val channelRegion: String,
    val metadata: ChatLogFileMetadata,
    val parsed: List<Token>,
)
