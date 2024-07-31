package dev.nohus.rift.characters.files

import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.GetLinuxSteamLibrariesUseCase
import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.nio.file.FileSystemException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Single
class GetEveSettingsDirectoryUseCase(
    private val operatingSystem: OperatingSystem,
    private val operatingSystemDirectories: OperatingSystemDirectories,
    private val getLinuxSteamLibrariesUseCase: GetLinuxSteamLibrariesUseCase,
    private val getEveCharactersSettingsUseCase: GetEveCharactersSettingsUseCase,
) {

    operator fun invoke(): Path? {
        val eveDataDirectory = when (operatingSystem) {
            OperatingSystem.Linux -> getLinuxEveDataDirectory()
            OperatingSystem.Windows -> getWindowsEveDataDirectory()
            OperatingSystem.MacOs -> getMacEveDataDirectory()
        } ?: return null

        return try {
            eveDataDirectory
                .listDirectoryEntries()
                .filter { file ->
                    file.isDirectory() && file.name.endsWith("_tranquility")
                }.maxByOrNull {
                    // Choose the directory where the newest character files are
                    getEveCharactersSettingsUseCase(it).maxOfOrNull { it.getLastModifiedTime().toMillis() } ?: 0
                } ?: return null
        } catch (e: FileSystemException) {
            logger.error(e) { "Failed reading EVE settings directory" }
            null
        }
    }

    private fun getLinuxEveDataDirectory(): Path? {
        val libraries = getLinuxSteamLibrariesUseCase()
        return libraries.map { library ->
            library.resolve("steamapps/compatdata/8500/pfx/drive_c/users/steamuser/AppData/Local/CCP/EVE")
        }.firstOrNull { it.exists() }
    }

    private fun getWindowsEveDataDirectory(): Path {
        val home = operatingSystemDirectories.getUserDirectory()
        return home.resolve("AppData/Local/CCP/EVE")
    }

    private fun getMacEveDataDirectory(): Path {
        val home = operatingSystemDirectories.getUserDirectory()
        return home.resolve("Library/Application Support/CCP/EVE")
    }
}
