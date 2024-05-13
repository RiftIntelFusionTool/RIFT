package dev.nohus.rift.utils.osdirectories

import org.apache.commons.lang3.SystemUtils
import java.nio.file.Path
import kotlin.io.path.createDirectories

class LinuxDirectories : OperatingSystemDirectories {

    override fun getUserDirectory(): Path {
        return Path.of(SystemUtils.USER_HOME)
    }

    override fun getAppConfigDirectory(applicationName: String): Path {
        return getConfigDirectory().resolve(applicationName).also { it.createDirectories() }
    }

    override fun getCacheDirectory(applicationName: String): Path {
        val cache = System.getenv("XDG_CACHE_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        if (cache != null) return Path.of(cache)
        return getUserDirectory().resolve(".cache/$applicationName").also { it.createDirectories() }
    }

    private fun getConfigDirectory(): Path {
        val configHome = System.getenv("XDG_CONFIG_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        if (configHome != null) return Path.of(configHome)
        return getUserDirectory().resolve(".config")
    }
}
