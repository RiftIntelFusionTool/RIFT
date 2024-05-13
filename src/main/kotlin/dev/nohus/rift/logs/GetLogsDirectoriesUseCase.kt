package dev.nohus.rift.logs

import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.GetLinuxSteamLibrariesUseCase
import org.apache.commons.lang3.SystemUtils
import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.exists

@Single
class GetLogsDirectoriesUseCase(
    private val operatingSystem: OperatingSystem,
    private val getLinuxSteamLibrariesUseCase: GetLinuxSteamLibrariesUseCase,
) {

    /**
     * Returns all EVE Online logs directories that can be found
     */
    operator fun invoke(): List<Path> {
        return when (operatingSystem) {
            OperatingSystem.Linux -> getLinuxLogsDirectories()
            OperatingSystem.Windows -> getWindowsLogsDirectories()
            OperatingSystem.MacOs -> getMacLogsDirectories()
        }.filter { it.exists() }
    }

    private fun getLinuxLogsDirectories(): List<Path> {
        val libraries = getLinuxSteamLibrariesUseCase()
        val logsInSteamLibraries = libraries.map { library ->
            library.resolve("steamapps/compatdata/8500/pfx/drive_c/users/steamuser/Documents/EVE/logs")
        }
        return listOf(getLogsInDocuments()) + logsInSteamLibraries
    }

    private fun getWindowsLogsDirectories(): List<Path> {
        return listOf(getLogsInDocuments(), getLogsInOneDrive())
    }

    private fun getMacLogsDirectories(): List<Path> {
        val homeDirectory = SystemUtils.getUserHome().toPath()
        val logsInWine = homeDirectory.resolve("Library/Application Support/EVE Online/p_drive/User/My Documents/EVE/logs")
        return listOf(getLogsInDocuments(), logsInWine)
    }

    private fun getLogsInDocuments(): Path {
        val homeDirectory = SystemUtils.getUserHome().toPath()
        return homeDirectory.resolve("Documents/EVE/logs")
    }

    private fun getLogsInOneDrive(): Path {
        val homeDirectory = SystemUtils.getUserHome().toPath()
        return homeDirectory.resolve("OneDrive/Documents/EVE/logs")
    }
}
