package dev.nohus.rift.logs

import org.koin.core.annotation.Single
import java.io.File

@Single
class GetChatLogsDirectoryUseCase {

    operator fun invoke(logsDirectory: File?): File? {
        return logsDirectory?.let { File(it, "Chatlogs") }?.takeIf { it.exists() }
    }
}
