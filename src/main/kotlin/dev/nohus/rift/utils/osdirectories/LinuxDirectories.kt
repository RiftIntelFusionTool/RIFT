package dev.nohus.rift.utils.osdirectories

import org.apache.commons.lang3.SystemUtils
import java.io.File

class LinuxDirectories : OperatingSystemDirectories {

    override fun getUserDirectory(): File {
        return File(SystemUtils.USER_HOME)
    }

    override fun getAppConfigDirectory(applicationName: String): File {
        return File(getConfigDirectory(), applicationName).also { it.mkdirs() }
    }

    override fun getCacheDirectory(applicationName: String): File {
        val cache = System.getenv("XDG_CACHE_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        if (cache != null) return File(cache)
        return File(getUserDirectory(), ".cache/$applicationName").also { it.mkdirs() }
    }

    private fun getConfigDirectory(): File {
        val configHome = System.getenv("XDG_CONFIG_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        if (configHome != null) return File(configHome)
        return File(getUserDirectory(), ".config")
    }
}
