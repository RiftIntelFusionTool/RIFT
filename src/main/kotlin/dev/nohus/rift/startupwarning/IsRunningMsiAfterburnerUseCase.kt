package dev.nohus.rift.startupwarning

import dev.nohus.rift.utils.IsWindowsProcessRunningUseCase
import dev.nohus.rift.utils.OperatingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class IsRunningMsiAfterburnerUseCase(
    private val operatingSystem: OperatingSystem,
    private val isWindowsProcessRunning: IsWindowsProcessRunningUseCase,
) {

    suspend operator fun invoke(): Boolean {
        if (operatingSystem != OperatingSystem.Windows) return false
        return listOf("MSIAfterburner.exe", "RTSSHooksLoader64.exe", "RTSS.exe").any {
            isWindowsProcessRunning(it)
        }.also {
            if (it) logger.warn { "Running MSI Afterburner" }
        }
    }
}
