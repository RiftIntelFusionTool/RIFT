package dev.nohus.rift.database

import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.Windows
import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private val logger = KotlinLogging.logger {}

@Single
class SqliteInitializer(
    operatingSystem: OperatingSystem,
    appDirectories: AppDirectories,
) {

    /**
     * The SQLite JDBC driver loads its native library by copying it to a temp directory first. On Windows,
     * this temp directory is in the users' home directory, and if the username contains non-ASCII characters,
     * then due to a JVM bug the native library will fail to load.
     *
     * This will detect that situation and prevent it by creating a custom temp directory
     * without the username in the path.
     */
    init {
        if (operatingSystem == Windows) {
            val cache = appDirectories.getAppCacheDirectory().absolutePathString()
            val isAscii = StandardCharsets.US_ASCII.newEncoder().canEncode(cache)
            if (!isAscii) {
                logger.info { "Cache directory contains non-ASCII characters: $cache" }
                val temp = getTempDirectory()
                if (temp != null) {
                    logger.info { "Setting SQLite temp directory to: ${temp.absolutePathString()}" }
                    System.setProperty("org.sqlite.tmpdir", temp.absolutePathString())
                } else {
                    logger.error { "Temp directory could not be created" }
                }
            }
        }
    }

    private fun getTempDirectory(): Path? {
        return listOf("Public", "ProgramData", "HomeDrive", "SystemDrive").mapNotNull { env ->
            Path.of(System.getenv(env)).takeIf { it.exists() && it.isDirectory() }
        }.asSequence().mapNotNull { path ->
            try {
                path.resolve("RIFT-Temp").createDirectories()
            } catch (e: IOException) {
                null
            }
        }.firstOrNull()
    }
}
