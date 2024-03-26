package dev.nohus.rift.utils.osdirectories

import org.apache.commons.lang3.SystemUtils
import java.io.File

class MacDirectories : OperatingSystemDirectories {

    override fun getUserDirectory(): File {
        return File(SystemUtils.USER_HOME)
    }

    override fun getAppConfigDirectory(applicationName: String): File {
        return File(getUserDirectory(), "Library/Preferences/$applicationName").also { it.mkdirs() }
    }

    override fun getCacheDirectory(applicationName: String): File {
        return File(getUserDirectory(), "Library/Caches/$applicationName").also { it.mkdirs() }
    }
}
