package dev.nohus.rift.utils.osdirectories

import java.io.File

interface OperatingSystemDirectories {
    fun getUserDirectory(): File
    fun getAppConfigDirectory(applicationName: String): File
    fun getCacheDirectory(applicationName: String): File
}
