package dev.nohus.rift.logs

import dev.nohus.rift.logs.DirectoryObserver.DirectoryObserverEvent.FileEvent
import dev.nohus.rift.logs.DirectoryObserver.DirectoryObserverEvent.OverflowEvent
import dev.nohus.rift.logs.DirectoryObserver.FileEventType.Created
import dev.nohus.rift.logs.parse.GameLogFileMetadata
import dev.nohus.rift.logs.parse.GameLogFileParser
import dev.nohus.rift.logs.parse.GameLogMessage
import dev.nohus.rift.logs.parse.GameLogMessageWithMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.FileSystemException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Single
class GameLogsObserver(
    private val directoryObserver: DirectoryObserver,
    private val matchGameLogFilenameUseCase: MatchGameLogFilenameUseCase,
    private val logFileParser: GameLogFileParser,
) {

    private val logFiles = mutableListOf<GameLogFile>()
    private val logFilesMutex = Mutex()
    private var activeLogFiles: Map<String, GameLogFileMetadata> = emptyMap() // String is the filename
    private var onMessageCallback: ((GameLogMessageWithMetadata) -> Unit)? = null
    private val handledMessages = mutableSetOf<GameLogMessage>()

    suspend fun observe(
        directory: Path,
        onCharacterLogin: suspend (characterId: Int) -> Unit,
        onMessage: (GameLogMessageWithMetadata) -> Unit,
    ) {
        logFilesMutex.withLock {
            logFiles.clear()
        }
        activeLogFiles = emptyMap()
        onMessageCallback = onMessage

        logger.info { "Observing game logs: $directory" }
        reloadLogFiles(directory)
        logger.debug { "Starting directory observer for game logs: $directory" }
        directoryObserver.observe(directory) { event ->
            when (event) {
                is FileEvent -> {
                    val logFile = matchGameLogFilenameUseCase(event.file)
                    if (logFile != null) {
                        when (event.type) {
                            Created -> {
                                logFilesMutex.withLock {
                                    logFiles += logFile
                                }
                                updateActiveLogFiles()
                                matchGameLogFilenameUseCase(event.file)?.characterId?.toIntOrNull()?.let { onCharacterLogin(it) }
                            }
                            DirectoryObserver.FileEventType.Deleted -> {
                                logFilesMutex.withLock {
                                    val file = logFiles.find { it.file.name == logFile.file.name }
                                    if (file != null) logFiles -= file
                                }
                                updateActiveLogFiles()
                            }
                            DirectoryObserver.FileEventType.Modified -> {
                                activeLogFiles[logFile.file.name]?.let { metadata ->
                                    readLogFile(logFile, metadata) // TODO: Optimise, we don't need to reread the file in full
                                }
                            }
                        }
                    }
                }
                OverflowEvent -> reloadLogFiles(directory)
            }
        }
        logger.info { "Stopped observing" }
    }

    fun stop() {
        directoryObserver.stop()
    }

    private suspend fun reloadLogFiles(directory: Path) {
        val logFiles = try {
            directory.listDirectoryEntries().mapNotNull { file ->
                matchGameLogFilenameUseCase(file)
            }
        } catch (e: FileSystemException) {
            logger.error(e) { "Failed reloading game log files" }
            emptyList()
        }
        logFilesMutex.withLock {
            this.logFiles.clear()
            this.logFiles.addAll(logFiles)
        }
        updateActiveLogFiles()
    }

    private suspend fun updateActiveLogFiles() {
        try {
            val minTime = Instant.now() - Duration.ofDays(7)
            val currentActiveLogFiles = logFilesMutex.withLock { logFiles.toList() }
                .filter { it.dateTime.toInstant(ZoneOffset.UTC).isAfter(minTime) }
                .groupBy { it.characterId }
                .mapNotNull { (characterId, playerLogFiles) ->
                    // Take the latest file for this player
                    val logFile = playerLogFiles
                        .sortedBy { it.lastModified }
                        .lastOrNull { it.file.exists() }
                        ?: return@mapNotNull null
                    val existingMetadata = activeLogFiles[logFile.file.name]
                    val metadata = existingMetadata ?: logFileParser.parseHeader(characterId, logFile.file)
                    if (metadata != null) {
                        logFile to metadata
                    } else {
                        logger.error { "Could not parse metadata for $logFile" }
                        null
                    }
                }

            val newActiveLogFiles = currentActiveLogFiles.filter { it.first.file.name !in activeLogFiles.keys }
            activeLogFiles = currentActiveLogFiles.associate { (logFile, metadata) -> logFile.file.name to metadata }

            newActiveLogFiles.forEach { (logFile, metadata) ->
                readLogFile(logFile, metadata)
            }
        } catch (e: IOException) {
            logger.error(e) { "Could not update active game log files" }
        }
    }

    private fun readLogFile(logFile: GameLogFile, metadata: GameLogFileMetadata) {
        try {
            val newMessages = logFileParser.parse(logFile.file).filter { it !in handledMessages }
            if (newMessages.isEmpty()) return
            handledMessages += newMessages
            newMessages.forEach { onMessageCallback?.invoke(GameLogMessageWithMetadata(it, metadata)) }
        } catch (e: IOException) {
            logger.error(e) { "Could not read game log file" }
        }
    }
}
