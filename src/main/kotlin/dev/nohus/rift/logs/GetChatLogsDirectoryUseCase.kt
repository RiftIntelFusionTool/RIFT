package dev.nohus.rift.logs

import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.exists

@Single
class GetChatLogsDirectoryUseCase {

    operator fun invoke(logsDirectory: Path?): Path? {
        return logsDirectory?.resolve("Chatlogs")?.takeIf { it.exists() }
    }
}
