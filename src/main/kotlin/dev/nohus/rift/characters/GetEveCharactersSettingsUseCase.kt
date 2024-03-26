package dev.nohus.rift.characters

import org.koin.core.annotation.Single
import java.io.File

@Single
class GetEveCharactersSettingsUseCase {

    private val regex = """core_char_[0-9]+""".toRegex()

    operator fun invoke(tranquilityDirectory: File?): List<File> {
        if (tranquilityDirectory == null) return emptyList()
        return (tranquilityDirectory.listFiles() ?: emptyArray())
            .filter { file ->
                file.isDirectory && file.name.startsWith("settings_")
            }.flatMap { directory ->
                (directory.listFiles() ?: emptyArray())
                    .filter { file -> file.isFile && file.extension == "dat" }
                    .filter { file -> file.nameWithoutExtension.matches(regex) }
            }
    }
}
