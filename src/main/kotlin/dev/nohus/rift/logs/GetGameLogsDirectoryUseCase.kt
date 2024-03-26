package dev.nohus.rift.logs

import org.koin.core.annotation.Single
import java.io.File

@Single
class GetGameLogsDirectoryUseCase {

    operator fun invoke(logsDirectory: File?): File? {
        return logsDirectory?.let { File(it, "Gamelogs") }?.takeIf { it.exists() }
    }
}
