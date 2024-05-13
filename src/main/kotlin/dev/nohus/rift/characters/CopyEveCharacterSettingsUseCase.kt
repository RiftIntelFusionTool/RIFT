package dev.nohus.rift.characters

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isWritable
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

@Single
class CopyEveCharacterSettingsUseCase {

    operator fun invoke(fromFile: Path, toFiles: List<Path>): Boolean {
        try {
            logger.info { "Copying character settings from $fromFile to $toFiles" }
            if (!fromFile.exists()) {
                logger.error { "Source character settings file does not exist" }
                return false
            }
            if (toFiles.any { !it.exists() }) {
                logger.error { "Target character settings file does not exist" }
                return false
            }
            val directory = fromFile.parent
            if (toFiles.any { it.parent != directory }) {
                logger.error { "Character settings files are not in the same directory" }
                return false
            }
            if (!directory.isWritable()) {
                logger.error { "Character settings directory is not writeable" }
                return false
            }

            toFiles.forEach { file ->
                val backup = getNewBackupFile(directory, file)
                file.copyTo(backup)
                file.deleteIfExists()
                fromFile.copyTo(file)
            }

            return true
        } catch (e: IOException) {
            logger.error(e) { "Copying character settings failed" }
            return false
        }
    }

    private fun getNewBackupFile(directory: Path, file: Path): Path {
        var count = 1
        while (true) {
            val backup = directory.resolve("${file.nameWithoutExtension}_rift_backup_$count.${file.extension}")
            if (!backup.exists()) return backup
            count++
        }
    }
}
