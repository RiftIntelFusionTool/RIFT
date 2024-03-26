package dev.nohus.rift.utils.osdirectories

import org.apache.commons.lang3.SystemUtils
import java.io.File

class WindowsDirectories : OperatingSystemDirectories {

    override fun getUserDirectory(): File {
        return File(SystemUtils.USER_HOME)
    }

    override fun getAppConfigDirectory(applicationName: String): File {
        return File(getAppDataDirectory(), applicationName).also { it.mkdirs() }
    }

    override fun getCacheDirectory(applicationName: String): File {
        return File(getAppConfigDirectory(applicationName), "cache").also { it.mkdirs() }
    }

    private fun getAppDataDirectory(): File {
        return File(System.getenv("AppData"))
    }
}
