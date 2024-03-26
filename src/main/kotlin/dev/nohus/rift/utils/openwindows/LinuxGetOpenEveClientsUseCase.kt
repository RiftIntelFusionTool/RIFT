package dev.nohus.rift.utils.openwindows

import dev.nohus.rift.utils.CommandRunner
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class LinuxGetOpenEveClientsUseCase(
    private val commandRunner: CommandRunner,
) : GetOpenEveClientsUseCase {

    private val regex = """ "EVE - (?<character>[A-z0-9 '-]{3,37})": """.toRegex()

    override fun invoke(): List<String>? {
        try {
            val output = commandRunner.run("xwininfo", "-root", "-children", "-tree").output
            return output.lines().mapNotNull { line ->
                val match = regex.find(line) ?: return@mapNotNull null
                match.groups["character"]!!.value
            }
        } catch (e: IllegalStateException) {
            logger.error(e) { "Could not get open Eve clients" }
            return null
        }
    }
}
