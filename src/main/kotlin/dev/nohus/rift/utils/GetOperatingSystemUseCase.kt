package dev.nohus.rift.utils

import org.apache.commons.lang3.SystemUtils
import org.koin.core.annotation.Single

@Single
class GetOperatingSystemUseCase {

    operator fun invoke(): OperatingSystem {
        return when {
            SystemUtils.IS_OS_LINUX -> OperatingSystem.Linux
            SystemUtils.IS_OS_MAC -> OperatingSystem.MacOs
            SystemUtils.IS_OS_WINDOWS -> OperatingSystem.Windows
            else -> OperatingSystem.Linux
        }
    }
}

enum class OperatingSystem {
    Linux, Windows, MacOs
}
