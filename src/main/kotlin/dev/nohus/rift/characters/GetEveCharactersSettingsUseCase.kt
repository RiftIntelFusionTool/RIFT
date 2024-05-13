package dev.nohus.rift.characters

import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@Single
class GetEveCharactersSettingsUseCase {

    private val regex = """core_char_[0-9]+""".toRegex()

    operator fun invoke(tranquilityDirectory: Path?): List<Path> {
        if (tranquilityDirectory == null) return emptyList()
        return tranquilityDirectory.listDirectoryEntries()
            .filter { file ->
                file.isDirectory() && file.name.startsWith("settings_")
            }.flatMap { directory ->
                directory.listDirectoryEntries()
                    .filter { file -> file.isRegularFile() && file.extension == "dat" }
                    .filter { file -> file.nameWithoutExtension.matches(regex) }
            }
    }
}
