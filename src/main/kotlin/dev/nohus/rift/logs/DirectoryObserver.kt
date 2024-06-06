package dev.nohus.rift.logs

import dev.nohus.rift.logs.DirectoryObserver.DirectoryObserverEvent.FileEvent
import dev.nohus.rift.logs.DirectoryObserver.DirectoryObserverEvent.OverflowEvent
import dev.nohus.rift.utils.OperatingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.koin.core.annotation.Factory
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

/**
 * Provides a callback informing about filesystem changes in a chosen directory
 */
@Factory
class DirectoryObserver(
    private val operatingSystem: OperatingSystem,
) {

    enum class FileEventType {
        Created, Deleted, Modified
    }

    sealed interface DirectoryObserverEvent {

        data class FileEvent(
            val file: Path,
            val type: FileEventType,
        ) : DirectoryObserverEvent

        data object OverflowEvent : DirectoryObserverEvent
    }

    private var watchJob: Job? = null

    suspend fun observe(directory: Path, onUpdate: suspend (DirectoryObserverEvent) -> Unit) = coroutineScope {
        stop()

        val watchService = directory.fileSystem.newWatchService()
        directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE, OVERFLOW)
        logger.debug { "Observing directory: $directory" }

        watchJob = launch {
            if (operatingSystem == OperatingSystem.Windows) {
                launch {
                    pokeFiles(directory)
                }
            }
            launch {
                while (true) {
                    val watchKey: WatchKey? = withContext(Dispatchers.IO) {
                        watchService.poll(1, TimeUnit.SECONDS)
                    }
                    if (watchKey != null) {
                        watchKey.pollEvents().forEach {
                            handleWatchEvent(directory, it, onUpdate)
                        }
                        watchKey.reset()
                    }
                    yield()
                }
            }
        }
    }

    /**
     * On Windows the file modification events are not consistently delivered.
     * Asking for the file size triggers Windows to deliver the event.
     */
    private suspend fun pokeFiles(path: Path) {
        while (true) {
            val recentFiles = path.toFile().listFiles()
                ?.filter {
                    Duration.between(Instant.ofEpochMilli(it.lastModified()), Instant.now()) < Duration.ofHours(2)
                }
                ?: emptyList()
            logger.debug { "Poking files: ${recentFiles.size}" }
            repeat(100) { // 100 * 200 == 20 seconds
                recentFiles.forEach { it.length() }
                delay(200)
            }
        }
    }

    fun stop() {
        watchJob?.cancel()
    }

    private suspend fun handleWatchEvent(directory: Path, watchEvent: WatchEvent<*>, onUpdate: suspend (DirectoryObserverEvent) -> Unit) {
        when (watchEvent.kind()) {
            ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE -> {
                val context = watchEvent.context() as Path
                val type = when (watchEvent.kind()) {
                    ENTRY_CREATE -> FileEventType.Created
                    ENTRY_MODIFY -> FileEventType.Modified
                    ENTRY_DELETE -> FileEventType.Deleted
                    else -> throw IllegalStateException()
                }
                val event = FileEvent(directory.resolve(context.name), type)
                logger.debug { "File update event: ${event.file.name} ${event.type}" }
                onUpdate(event)
            }
            OVERFLOW -> {
                onUpdate(OverflowEvent)
            }
        }
    }
}
