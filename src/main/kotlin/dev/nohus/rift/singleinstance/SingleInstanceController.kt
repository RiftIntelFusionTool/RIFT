package dev.nohus.rift.singleinstance

import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class SingleInstanceController(
    appDirectories: AppDirectories,
) {

    private val lockFile = File(appDirectories.getAppDataDirectory(), ".lock")
    private val keepAliveDuration = Duration.ofSeconds(1)

    suspend fun start() = coroutineScope {
        while (true) {
            val text = Instant.now().toEpochMilli().toString()
            try {
                lockFile.writeText(text)
            } catch (ignored: IOException) {
                logger.error(ignored) { "Could not write to lock file" }
            }
            delay(keepAliveDuration.toMillis())
        }
    }

    fun isInstanceRunning(): Boolean {
        return try {
            val millis = lockFile.readText().toLongOrNull() ?: return false
            val instant = Instant.ofEpochMilli(millis)
            val duration = Duration.between(instant, Instant.now())
            duration < keepAliveDuration
        } catch (e: IOException) {
            false
        }
    }
}
