package dev.nohus.rift.utils

import dev.nohus.rift.utils.OperatingSystem.Windows
import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.nio.charset.StandardCharsets
import kotlin.io.path.absolutePathString

private val logger = KotlinLogging.logger {}

@Single
class HasNonAsciiWindowsUsernameUseCase(
    private val operatingSystem: OperatingSystem,
    private val appDirectories: AppDirectories,
) {

    operator fun invoke(): Boolean {
        if (operatingSystem == Windows) {
            val cache = appDirectories.getAppCacheDirectory().absolutePathString()
            val isAscii = StandardCharsets.US_ASCII.newEncoder().canEncode(cache)
            if (!isAscii) {
                logger.info { "Cache directory contains non-ASCII characters: $cache" }
                return true
            }
        }
        return false
    }
}
