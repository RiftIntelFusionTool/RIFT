package dev.nohus.rift.characters.files

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class DetectEveSettingsDirectoryUseCase(
    private val getEveSettingsDirectoryUseCase: GetEveSettingsDirectoryUseCase,
    private val settings: Settings,
) {

    /**
     * Detects EVE Online character settings directories and updates settings with it, overwriting any existing setting
     */
    operator fun invoke(): Path? {
        val directory = getEveSettingsDirectoryUseCase()
        if (directory != null) settings.eveSettingsDirectory = directory
        return directory
    }
}
