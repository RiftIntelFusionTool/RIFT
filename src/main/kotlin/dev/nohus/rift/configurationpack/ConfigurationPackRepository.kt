package dev.nohus.rift.configurationpack

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.ConfigurationPack.Imperium
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class ConfigurationPackRepository(
    private val settings: Settings,
    private val localCharactersRepository: LocalCharactersRepository,
) {
    data class SuggestedIntelChannels(
        val promptTitleText: String,
        val promptButtonText: String,
        val channels: List<IntelChannel>,
    )

    fun set(configurationPack: ConfigurationPack?) {
        if (settings.configurationPack != configurationPack) {
            settings.configurationPack = configurationPack
            settings.isConfigurationPackReminderDismissed = false
        }
    }

    fun getSuggestedPack(): ConfigurationPack? {
        val characterAlliances = localCharactersRepository.characters.value
            .mapNotNull { it.info.success?.allianceId }
            .toSet()
        return ConfigurationPack.entries.firstOrNull { pack ->
            getFriendlyAllianceIds(pack).any { it in characterAlliances }
        }
    }

    private fun getFriendlyAllianceIds(pack: ConfigurationPack?): List<Int> {
        return when (pack) {
            Imperium -> listOf(
                99003214, // Brave Collective
                99009163, // Dracarys.
                99012042, // Fanatic Legion.
                150097440, // Get Off My Lawn
                1354830081, // Goonswarm Federation
                131511956, // Tactical Narcotics Team
                99003995, // Invidia Gloriae Comes
                99009331, // Scumlords
                99010140, // Stribog Clade
                99011162, // Shadow Ultimatum
                99011223, // Sigma Grindset
                99010931, // WE FORM BL0B
            )
            null -> emptyList()
        }
    }

    fun getSuggestedIntelChannels(): SuggestedIntelChannels? {
        return when (settings.configurationPack) {
            Imperium -> SuggestedIntelChannels(
                promptTitleText = "Would you like intel channels of The Imperium to be configured automatically?",
                promptButtonText = "Add Imperium channels",
                channels = listOf(
                    IntelChannel("delve.imperium", "Delve"),
                    IntelChannel("ftn.imperium", "Fountain"),
                    IntelChannel("querious.imperium", "Querious"),
                    IntelChannel("period.imperium", "Period Basis"),
                    IntelChannel("catch.imperium", "Catch"),
                    IntelChannel("cr.imperium", "Cloud Ring"),
                    IntelChannel("paragon.imperium", "Paragon Soul"),
                    IntelChannel("esoteria.imperium", "Esoteria"),
                    IntelChannel("triangle.imperium", "Pochven"),
                    IntelChannel("khanid.imperium", "Khanid"),
                    IntelChannel("aridia.imperium", "Aridia"),
                ),
            )
            null -> null
        }
    }

    fun isJabberEnabled(): Boolean {
        return when (settings.configurationPack) {
            Imperium -> true
            null -> false
        }
    }

    fun isFriendlyAlliance(allianceId: Int): Boolean {
        return allianceId in getFriendlyAllianceIds(settings.configurationPack)
    }

    fun getJumpBridgeNetworkUrl(): String? {
        return when (settings.configurationPack) {
            Imperium -> "https://wiki.goonswarm.org/w/Alliance:Stargate"
            null -> null
        }
    }
}
