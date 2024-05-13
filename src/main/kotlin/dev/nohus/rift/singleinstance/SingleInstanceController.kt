package dev.nohus.rift.singleinstance

import dev.nohus.rift.utils.directories.AppDirectories
import org.koin.core.annotation.Single
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

@Single
class SingleInstanceController(
    appDirectories: AppDirectories,
) {

    private val lockFile = appDirectories.getAppDataDirectory().resolve(".lock")
    private var lock: FileLock? = FileChannel.open(lockFile, WRITE, CREATE).tryLock()

    fun isInstanceRunning(): Boolean {
        return lock == null
    }
}
