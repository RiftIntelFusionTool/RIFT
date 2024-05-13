package dev.nohus.rift.configurationpack

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class ShouldShowConfigurationPackReminderUseCase(
    private val localCharactersRepository: LocalCharactersRepository,
    private val configurationPackRepository: ConfigurationPackRepository,
    private val settings: Settings,
) {

    suspend operator fun invoke(): Boolean {
        if (settings.isConfigurationPackReminderDismissed) return false
        if (!settings.isSetupWizardFinished) return false
        if (settings.authenticatedCharacters.isEmpty()) return false
        if (settings.configurationPack != null) return false
        localCharactersRepository.characters.first { characters ->
            characters.isNotEmpty() && characters.none { it.info is AsyncResource.Loading }
        }
        val suggested = configurationPackRepository.getSuggestedPack()
        return suggested != null
    }
}
