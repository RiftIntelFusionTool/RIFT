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
class GameLogFileParser {

    private val gameLogMessageRegex = """^\[ (?<datetime>[0-9.]+ [0-9:]+) ] \((?<type>.+)\) (?<message>.*)$""".toRegex()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    private val charset = Charsets.UTF_8

    fun parse(file: Path): List<GameLogMessage> {
        val text = file.readText(charset)
        return parseText(text)
    }

    fun parseHeader(characterId: String, file: Path): GameLogFileMetadata? {
        val metadata = mutableMapOf<String, String>()

        try {
            file.bufferedReader(charset).useLines { lines ->
                var inHeader = false
                for (line in lines) {
                    if (line.length <= 1) continue
                    if (line == "------------------------------------------------------------") {
                        inHeader = !inHeader
                    } else if (inHeader) {
                        if (line == "  Gamelog") continue
                        val key = line.substringBefore(":").trim()
                        val value = line.substringAfter(":").trim()
                        metadata[key] = value
                    } else {
                        break
                    }
                }
            }
        } catch (e: IOException) {
            return null
        }

        return GameLogFileMetadata(
            listener = metadata["Listener"] ?: return null,
            characterId = characterId.toIntOrNull() ?: return null,
            sessionStarted = metadata["Session Started"] ?: return null,
        )
    }

    private fun parseText(text: String): List<GameLogMessage> {
        return text.lines().mapNotNull(::parseLine)
    }

    private fun parseLine(line: String): GameLogMessage? {
        val match = gameLogMessageRegex.find(line) ?: return null
        val datetime = match.groups["datetime"]!!.value
        val timestamp = LocalDateTime.parse(datetime, dateFormatter).toInstant(ZoneOffset.UTC)
        val type = match.groups["type"]!!.value
        val message = match.groups["message"]!!.value
        return GameLogMessage(timestamp, type, message)
    }
}
