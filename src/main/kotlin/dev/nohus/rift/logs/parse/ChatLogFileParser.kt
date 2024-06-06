package dev.nohus.rift.logs.parse

import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.path.bufferedReader
import kotlin.io.path.readText

@Single
class ChatLogFileParser {

    private val chatMessageRegex = """^\uFEFF\[ (?<datetime>[0-9.]+ [0-9:]+) ] (?<author>[^>]+) > (?<message>.*)$""".toRegex()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    private val charset = Charsets.UTF_16LE

    fun parse(file: Path): List<ChatMessage> {
        val text = file.readText(charset)
        return parseText(text)
    }

    fun parseHeader(characterId: String, file: Path): ChatLogFileMetadata? {
        val metadata = mutableMapOf<String, String>()
        val systemMessages = mutableListOf<ChatMessage>()

        try {
            file.bufferedReader(charset).useLines { lines ->
                var inHeader = false
                for (line in lines) {
                    if (line.length <= 1) continue
                    if (line == "        ---------------------------------------------------------------") {
                        inHeader = !inHeader
                    } else if (inHeader) {
                        val key = line.substringBefore(":").trim()
                        val value = line.substringAfter(":").trim()
                        metadata[key] = value
                    } else {
                        val message = parseLine(line)
                        if (message?.author == "EVE System") {
                            systemMessages += message
                        } else {
                            break
                        }
                    }
                }
            }
        } catch (e: IOException) {
            return null
        }

        if (systemMessages.isNotEmpty()) {
            for (message in systemMessages) {
                val text = message.message
                when {
                    text.startsWith("Channel changed to Corp : ") -> metadata["GroupName"] = text.substringAfter(" : ")
                    text.startsWith("Channel changed to Alliance : ") -> metadata["GroupName"] = text.substringAfter(" : ")
                    text.startsWith("Channel MOTD: ") -> metadata["MOTD"] = text.substringAfter(": ")
                }
            }
        }

        return ChatLogFileMetadata(
            channelId = metadata["Channel ID"] ?: return null,
            channelName = metadata["Channel Name"] ?: return null,
            listener = metadata["Listener"] ?: return null,
            characterId = characterId.toIntOrNull() ?: return null,
            sessionStarted = metadata["Session started"] ?: return null,
            groupName = metadata["GroupName"],
            motd = metadata["MOTD"],
        )
    }

    private fun parseText(text: String): List<ChatMessage> {
        return text.lines().mapNotNull(::parseLine)
    }

    private fun parseLine(line: String): ChatMessage? {
        val match = chatMessageRegex.find(line) ?: return null
        val datetime = match.groups["datetime"]!!.value
        val timestamp = LocalDateTime.parse(datetime, dateFormatter).toInstant(ZoneOffset.UTC)
        val author = match.groups["author"]!!.value
        val message = match.groups["message"]!!.value
        return ChatMessage(timestamp, author, message)
    }
}
