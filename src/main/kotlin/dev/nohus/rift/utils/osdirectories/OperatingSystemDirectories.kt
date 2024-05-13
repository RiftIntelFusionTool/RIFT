package dev.nohus.rift.utils.osdirectories

import java.nio.file.Path

interface OperatingSystemDirectories {
    fun getUserDirectory(): Path
    fun getAppConfigDirectory(applicationName: String): Path
    fun getCacheDirectory(applicationName: String): Path
}
