package dev.nohus.rift.utils.directories

import org.apache.commons.lang3.SystemUtils
import org.koin.core.annotation.Single
import java.io.File

@Single
class GetLinuxSteamLibrariesUseCase(
    private val getLinuxPartitionsUseCase: GetLinuxPartitionsUseCase,
) {

    private val libraryDirectoryName = "SteamLibrary"
    private val homeLibraryPath = ".local/share/Steam"

    /**
     * Returns a list of Steam library directories
     */
    operator fun invoke(): List<File> {
        val partitions = getLinuxPartitionsUseCase()
        val homeDirectory = SystemUtils.getUserHome()
        val homeLibrary = File(homeDirectory, homeLibraryPath)
        val additionalLibraries = partitions.map { partition ->
            File(partition, libraryDirectoryName)
        }
        val libraries = additionalLibraries + listOf(homeLibrary)
        return libraries.filter { it.exists() }
    }
}
