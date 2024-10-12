package dev.nohus.rift.pings

import dev.nohus.rift.network.HttpGetUseCase
import dev.nohus.rift.utils.CommandRunner
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.get
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.awt.Desktop

@Single
class OpenMumbleUseCase(
    private val httpGet: HttpGetUseCase,
    private val commandRunner: CommandRunner,
    private val operatingSystem: OperatingSystem,
) {

    private val regex = """window.location = '(?<redirect>.*)'""".toRegex()

    suspend operator fun invoke(link: String) {
        val response = httpGet(link).success
        if (response != null) {
            val match = regex.find(response)
            if (match != null) {
                val redirect = match["redirect"].toURIOrNull()
                if (redirect != null) {
                    when (operatingSystem) {
                        OperatingSystem.Linux -> {
                            commandRunner.runAsync("xdg-open", redirect.toString())
                        }
                        OperatingSystem.Windows, OperatingSystem.MacOs -> {
                            withContext(Dispatchers.IO) {
                                Desktop.getDesktop().browse(redirect)
                            }
                        }
                    }
                    return
                }
            }
        }
        link.toURIOrNull()?.openBrowser()
    }
}
