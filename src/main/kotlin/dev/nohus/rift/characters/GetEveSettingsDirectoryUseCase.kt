package dev.nohus.rift.characters

import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.GetLinuxSteamLibrariesUseCase
import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import org.koin.core.annotation.Single
import java.io.File

@Single
class GetEveSettingsDirectoryUseCase(
    private val operatingSystem: OperatingSystem,
    private val operatingSystemDirectories: OperatingSystemDirectories,
    private val getLinuxSteamLibrariesUseCase: GetLinuxSteamLibrariesUseCase,
) {

    operator fun invoke(): File? {
        val eveDataDirectory = when (operatingSystem) {
            OperatingSystem.Linux -> getLinuxEveDataDirectory()
            OperatingSystem.Windows -> getWindowsEveDataDirectory()
            OperatingSystem.MacOs -> getMacEveDataDirectory()
        } ?: return null

        val tranquilityDirectory = (eveDataDirectory.listFiles() ?: emptyArray())
            .firstOrNull { file ->
                file.isDirectory && file.name.endsWith("_tranquility")
            } ?: return null

        return tranquilityDirectory
    }

    private fun getLinuxEveDataDirectory(): File? {
        val libraries = getLinuxSteamLibrariesUseCase()
        return libraries.map { library ->
            File(library, "steamapps/compatdata/8500/pfx/drive_c/users/steamuser/AppData/Local/CCP/EVE")
        }.firstOrNull { it.exists() }
    }

    private fun getWindowsEveDataDirectory(): File {
        val home = operatingSystemDirectories.getUserDirectory()
        return File(home, "AppData/Local/CCP/EVE")
    }

    private fun getMacEveDataDirectory(): File {
        val home = operatingSystemDirectories.getUserDirectory()
        return File(home, "Library/Application Support/CCP/EVE")
    }
}
