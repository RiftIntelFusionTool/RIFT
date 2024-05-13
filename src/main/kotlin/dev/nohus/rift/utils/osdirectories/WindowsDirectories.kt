package dev.nohus.rift.utils.osdirectories

import org.apache.commons.lang3.SystemUtils
import java.nio.file.Path
import kotlin.io.path.createDirectories

class WindowsDirectories : OperatingSystemDirectories {

    override fun getUserDirectory(): Path {
        return Path.of(SystemUtils.USER_HOME)
    }

    override fun getAppConfigDirectory(applicationName: String): Path {
        return getAppDataDirectory().resolve(applicationName).also { it.createDirectories() }
    }

    override fun getCacheDirectory(applicationName: String): Path {
        return getAppConfigDirectory(applicationName).resolve("cache").also { it.createDirectories() }
    }

    private fun getAppDataDirectory(): Path {
        return Path.of(System.getenv("AppData"))
    }
}
