package dev.nohus.rift.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class IsWindowsProcessRunningUseCase(
    private val commandRunner: CommandRunner,
) {
    suspend operator fun invoke(processName: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                /**
                 * /fo csv - format output to CSV
                 * /nh - don't display headers
                 * /fi - filter processes
                 */
                val result = commandRunner.run(
                    "tasklist.exe",
                    "/fo",
                    "csv",
                    "/nh",
                    "/fi",
                    "\"IMAGENAME eq $processName\"",
                )
                processName in result.output
            }
        } catch (e: IllegalStateException) {
            logger.error { "Failed getting processes: ${e.message}" }
            false
        }
    }
}
