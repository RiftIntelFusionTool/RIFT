package dev.nohus.rift.logs

import org.koin.core.annotation.Single
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Single
class MatchGameLogFilenameUseCase {

    private val gameLogFilenameRegex = """^(?<date>[0-9]+)_(?<time>[0-9]+)_(?<playerid>[0-9]+)\.txt$""".toRegex()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    operator fun invoke(file: File): GameLogFile? {
        val match = gameLogFilenameRegex.find(file.name) ?: return null
        val date = match.groups["date"]!!.value
        val time = match.groups["time"]!!.value
        val dateTime = LocalDateTime.parse("$date$time", dateFormatter)
        val playerId = match.groups["playerid"]!!.value
        val lastModifier = Instant.ofEpochMilli(file.lastModified())
        return GameLogFile(file, dateTime, playerId, lastModifier)
    }
}
