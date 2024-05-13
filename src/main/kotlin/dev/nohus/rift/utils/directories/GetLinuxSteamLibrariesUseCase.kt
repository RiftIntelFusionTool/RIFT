package dev.nohus.rift.utils.directories

import org.apache.commons.lang3.SystemUtils
import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.exists

@Single
class GetLinuxSteamLibrariesUseCase(
    private val getLinuxPartitionsUseCase: GetLinuxPartitionsUseCase,
) {

    private val libraryDirectoryName = "SteamLibrary"
    private val homeLibraryPath = ".local/share/Steam"

    /**
     * Returns a list of Steam library directories
     */
    operator fun invoke(): List<Path> {
        val partitions = getLinuxPartitionsUseCase()
        val homeDirectory = SystemUtils.getUserHome().toPath()
        val homeLibrary = homeDirectory.resolve(homeLibraryPath)
        val additionalLibraries = partitions.map { partition ->
            partition.resolve(libraryDirectoryName)
        }
        val libraries = additionalLibraries + listOf(homeLibrary)
        return libraries.filter { it.exists() }
    }
}
