package dev.nohus.rift.utils.directories

import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import org.koin.core.annotation.Single
import java.io.File

@Single
class AppDirectories(
    private val operatingSystemDirectories: OperatingSystemDirectories,
) {

    private val applicationName = "RIFT"

    fun getAppDataDirectory(): File {
        return operatingSystemDirectories.getAppConfigDirectory(applicationName)
    }

    fun getAppCacheDirectory(): File {
        return operatingSystemDirectories.getCacheDirectory(applicationName)
    }
}
