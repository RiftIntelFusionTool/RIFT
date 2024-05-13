package dev.nohus.rift.utils.directories

import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class AppDirectories(
    private val operatingSystemDirectories: OperatingSystemDirectories,
) {

    private val applicationName = "RIFT"

    fun getAppDataDirectory(): Path {
        return operatingSystemDirectories.getAppConfigDirectory(applicationName)
    }

    fun getAppCacheDirectory(): Path {
        return operatingSystemDirectories.getCacheDirectory(applicationName)
    }
}
