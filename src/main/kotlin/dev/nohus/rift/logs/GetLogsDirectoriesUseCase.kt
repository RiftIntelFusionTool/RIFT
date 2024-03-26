package dev.nohus.rift.logs

import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.GetLinuxSteamLibrariesUseCase
import org.apache.commons.lang3.SystemUtils
import org.koin.core.annotation.Single
import java.io.File

@Single
class GetLogsDirectoriesUseCase(
    private val operatingSystem: OperatingSystem,
    private val getLinuxSteamLibrariesUseCase: GetLinuxSteamLibrariesUseCase,
) {

    /**
     * Returns all EVE Online logs directories that can be found
     */
    operator fun invoke(): List<File> {
        return when (operatingSystem) {
            OperatingSystem.Linux -> getLinuxLogsDirectories()
            OperatingSystem.Windows -> getWindowsLogsDirectories()
            OperatingSystem.MacOs -> getMacLogsDirectories()
        }.filter { it.exists() }
    }

    private fun getLinuxLogsDirectories(): List<File> {
        val libraries = getLinuxSteamLibrariesUseCase()
        val logsInSteamLibraries = libraries.map { library ->
            File(library, "steamapps/compatdata/8500/pfx/drive_c/users/steamuser/Documents/EVE/logs")
        }
        return listOf(getLogsInDocuments()) + logsInSteamLibraries
    }

    private fun getWindowsLogsDirectories(): List<File> {
        return listOf(getLogsInDocuments(), getLogsInOneDrive())
    }

    private fun getMacLogsDirectories(): List<File> {
        val homeDirectory = SystemUtils.getUserHome()
        val logsInWine = File(homeDirectory, "Library/Application Support/EVE Online/p_drive/User/My Documents/EVE/logs")
        return listOf(getLogsInDocuments(), logsInWine)
    }

    private fun getLogsInDocuments(): File {
        val homeDirectory = SystemUtils.getUserHome()
        return File(homeDirectory, "Documents/EVE/logs")
    }

    private fun getLogsInOneDrive(): File {
        val homeDirectory = SystemUtils.getUserHome()
        return File(homeDirectory, "OneDrive/Documents/EVE/logs")
    }
}
