package dev.nohus.rift.utils.osdirectories

import org.apache.commons.lang3.SystemUtils
import java.nio.file.Path
import kotlin.io.path.createDirectories

class MacDirectories : OperatingSystemDirectories {

    override fun getUserDirectory(): Path {
        return Path.of(SystemUtils.USER_HOME)
    }

    override fun getAppConfigDirectory(applicationName: String): Path {
        return getUserDirectory().resolve("Library/Preferences/$applicationName").also { it.createDirectories() }
    }

    override fun getCacheDirectory(applicationName: String): Path {
        return getUserDirectory().resolve("Library/Caches/$applicationName").also { it.createDirectories() }
    }
}
