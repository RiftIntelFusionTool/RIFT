package dev.nohus.rift.logs

import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Single
class MatchChatLogFilenameUseCase {

    private val chatLogFilenameRegex = """^(?<name>.*)_(?<date>[0-9]{8})_(?<time>[0-9]{6})(_(?<characterid>[0-9]+))?\.txt$""".toRegex()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    operator fun invoke(file: Path): ChatLogFile? {
        try {
            val match = chatLogFilenameRegex.find(file.name) ?: return null
            val name = match.groups["name"]!!.value
            val date = match.groups["date"]!!.value
            val time = match.groups["time"]!!.value
            val characterId = match.groups["characterid"]?.value ?: return null // Old log files do not contain the character ID
            val dateTime = LocalDateTime.parse("$date$time", dateFormatter)
            try {
                val lastModified = file.getLastModifiedTime().toInstant()
                return ChatLogFile(file, name, dateTime, characterId, lastModified)
            } catch (e: IOException) {
                logger.error(e) { "Chat log file not found" }
                return null
            }
        } catch (e: DateTimeParseException) {
            Sentry.captureException(IllegalArgumentException("Could not parse chat log filename: ${file.name}", e))
            return null
        }
    }
}
