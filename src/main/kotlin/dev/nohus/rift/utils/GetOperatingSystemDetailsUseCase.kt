package dev.nohus.rift.utils

import org.koin.core.annotation.Single

@Single
class GetOperatingSystemDetailsUseCase(
    private val operatingSystem: OperatingSystem,
) {

    operator fun invoke(): String? {
        return when (operatingSystem) {
            OperatingSystem.Linux -> getLinuxVersion()
            OperatingSystem.Windows -> getWindowsVersion()
            OperatingSystem.MacOs -> null
        }
    }

    private fun getLinuxVersion(): String? {
        val result = CommandRunner().run("lsb_release", "-ds", ignoreFailure = true, timeout = 500)
        return if (result.exitStatus == 0) {
            result.output.trim()
        } else {
            null
        }
    }

    private fun getWindowsVersion(): String? {
        val result = CommandRunner().run("cmd.exe", "/c", "ver", ignoreFailure = true, timeout = 500)
        return if (result.exitStatus == 0) {
            result.output.trim().substringAfter("[").substringBefore("]").substringAfter(" ")
        } else {
            null
        }
    }
}
