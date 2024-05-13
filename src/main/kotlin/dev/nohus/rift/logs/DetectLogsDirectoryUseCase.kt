package dev.nohus.rift.logs

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class DetectLogsDirectoryUseCase(
    private val getLogsDirectoriesUseCase: GetLogsDirectoriesUseCase,
    private val settings: Settings,
) {

    /**
     * Detects an EVE Online logs directory and updates settings with it, overwriting any existing setting
     */
    operator fun invoke(): Path? {
        val logsDirectories = getLogsDirectoriesUseCase()
        val logsDirectory = logsDirectories.firstOrNull() // TODO: Pick best, not first
        if (logsDirectory != null) settings.eveLogsDirectory = logsDirectory
        return logsDirectory
    }
}
