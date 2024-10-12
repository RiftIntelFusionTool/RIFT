package dev.nohus.rift.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

@Single
class CleanupTempFilesUseCase {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        val maxAge = Instant.now() - Duration.ofDays(7)
        try {
            val path = Path.of(System.getProperty("java.io.tmpdir"))
            path
                .listDirectoryEntries("sqlite-jdbc-tmp-*.db")
                .filter { it.getLastModifiedTime().toInstant().isBefore(maxAge) }
                .forEach { entry ->
                    logger.debug { "Cleaning up old sqlite temp file: $entry (${entry.getLastModifiedTime()})" }
                    entry.deleteExisting()
                }
            path
                .listDirectoryEntries("sqlite-*-*sqlitejdbc.{so,dll}")
                .forEach { entry ->
                    val lockFile = Path.of("${entry.pathString}.lck")
                    if (!lockFile.exists()) {
                        logger.debug { "Cleaning up old sqlite library file: $entry (${entry.getLastModifiedTime()})" }
                        entry.deleteExisting()
                    }
                }
        } catch (e: InvalidPathException) {
            logger.error(e) { "Could not search for temp files to cleanup" }
        } catch (e: IOException) {
            logger.error(e) { "Could not cleanup temp files" }
        }
    }
}
