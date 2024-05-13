package dev.nohus.rift.utils.directories

import dev.nohus.rift.characters.files.DetectEveSettingsDirectoryUseCase
import dev.nohus.rift.logs.DetectLogsDirectoryUseCase
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class DetectDirectoriesUseCase(
    private val detectLogsDirectoryUseCase: DetectLogsDirectoryUseCase,
    private val detectEveSettingsDirectoryUseCase: DetectEveSettingsDirectoryUseCase,
    private val settings: Settings,
) {

    operator fun invoke() {
        if (settings.eveLogsDirectory == null) detectLogsDirectoryUseCase()
        if (settings.eveSettingsDirectory == null) detectEveSettingsDirectoryUseCase()
    }
}
