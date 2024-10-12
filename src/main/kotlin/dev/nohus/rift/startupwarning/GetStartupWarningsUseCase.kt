package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class GetStartupWarningsUseCase(
    private val hasNonEnglishEveClient: HasNonEnglishEveClientUseCase,
    private val isRunningMsiAfterburner: IsRunningMsiAfterburnerUseCase,
    private val settings: Settings,
) {

    data class StartupWarning(
        val id: String,
        val title: String,
        val description: String,
    )

    suspend operator fun invoke(): List<StartupWarning> {
        return buildList {
            if (hasNonEnglishEveClient()) {
                add(
                    StartupWarning(
                        id = "non-english client",
                        title = "Non-English EVE Client",
                        description = """
                            Your EVE client is set to a language other than English.
                            RIFT features based on reading game logs won't work.
                        """.trimIndent(),
                    ),
                )
            }
            if (isRunningMsiAfterburner()) {
                add(
                    StartupWarning(
                        id = "msi afterburner",
                        title = "MSI Afterburner",
                        description = """
                            You are running MSI Afterburner or RivaTuner, which is known to inject code into RIFT that causes freezes and crashes.
                        """.trimIndent(),
                    ),
                )
            }
        }.filter { it.id !in settings.dismissedWarnings }
    }
}
