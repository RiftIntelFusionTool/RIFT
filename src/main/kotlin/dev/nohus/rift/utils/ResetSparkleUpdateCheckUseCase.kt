package dev.nohus.rift.utils

import org.koin.core.annotation.Single

@Single
class ResetSparkleUpdateCheckUseCase(
    private val operatingSystem: OperatingSystem,
    private val commandRunner: CommandRunner,
) {

    /**
     * Force Sparkle to recheck for updates on next start
     */
    operator fun invoke() {
        if (operatingSystem == OperatingSystem.MacOs) {
            commandRunner.run("defaults", "delete", "dev.nohus.rift", "SULastCheckTime")
        }
    }
}
