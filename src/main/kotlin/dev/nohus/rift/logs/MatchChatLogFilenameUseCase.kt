package dev.nohus.rift.logs

import io.sentry.Sentry
import org.koin.core.annotation.Single
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Single
class MatchChatLogFilenameUseCase {

    private val chatLogFilenameRegex = """^(?<name>.*)_(?<date>[0-9]{8})_(?<time>[0-9]{6})(_(?<characterid>[0-9]+))?\.txt$""".toRegex()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    operator fun invoke(file: File): ChatLogFile? {
        try {
            val match = chatLogFilenameRegex.find(file.name) ?: return null
            val name = match.groups["name"]!!.value
            val date = match.groups["date"]!!.value
            val time = match.groups["time"]!!.value
            val characterId = match.groups["characterid"]?.value ?: return null // Old log files do not contain the character ID
            val dateTime = LocalDateTime.parse("$date$time", dateFormatter)
            val lastModifier = Instant.ofEpochMilli(file.lastModified())
            return ChatLogFile(file, name, dateTime, characterId, lastModifier)
        } catch (e: DateTimeParseException) {
            Sentry.captureException(IllegalArgumentException("Could not parse chat log filename: ${file.name}", e))
            return null
        }
    }
}
