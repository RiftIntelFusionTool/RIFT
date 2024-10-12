package dev.nohus.rift.standings

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.network.esi.Contact
import dev.nohus.rift.network.esi.ContactType
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class StandingsRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val settings: Settings,
) {

    @Serializable
    data class Standings(
        val alliance: Map<Int, Float> = emptyMap(),
        val corporation: Map<Int, Float> = emptyMap(),
        val character: Map<Int, Float> = emptyMap(),
    )

    private val standings get() = settings.standings
    private var lastUpdated: Instant = Instant.EPOCH

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters.debounce(500).collect {
                updateStandings()
            }
        }
        launch {
            while (true) {
                delay(2.minutes)
                if (Duration.between(lastUpdated, Instant.now()) > Duration.ofMinutes(15)) {
                    updateStandings()
                }
            }
        }
    }

    fun getStanding(allianceId: Int?, corporationId: Int?, characterId: Int?): Standing {
        standings.character[characterId]?.let { return getStandingLevel(it) }
        standings.corporation[corporationId]?.let { return getStandingLevel(it) }
        standings.alliance[allianceId]?.let { return getStandingLevel(it) }
        return Standing.Neutral
    }

    fun getFriendlyAllianceIds(): Set<Int> {
        return standings.alliance.filter { it.value > 0f }.keys
    }

    private fun getStandingLevel(standing: Float): Standing {
        return when {
            standing > 5.0f -> Standing.Excellent
            standing > 0f -> Standing.Good
            standing == 0f -> Standing.Neutral
            standing >= -5.0f -> Standing.Bad
            else -> Standing.Terrible
        }
    }

    private suspend fun updateStandings() {
        val validCharacters = localCharactersRepository.characters.value.filter { it.isAuthenticated && it.info.success != null }
        if (validCharacters.isEmpty()) return
        val standings = getStandings(validCharacters)
        if (standings != null) {
            settings.standings = standings
            lastUpdated = Instant.now()
            logger.info { "Updated standings" }
        } else {
            logger.error { "Could not update standings" }
        }
    }

    private suspend fun getStandings(characters: List<LocalCharactersRepository.LocalCharacter>): Standings? {
        val allianceIds = mutableMapOf<Int, Int>()
        val corporationIds = mutableMapOf<Int, Int>()
        val characterIds = mutableListOf<Int>()
        characters.forEach { character ->
            character.info.success?.let { details ->
                if (details.allianceId != null) allianceIds[details.allianceId] = character.characterId
                corporationIds[details.corporationId] = character.characterId
                characterIds += character.characterId
            }
        }
        val contacts = getContacts(allianceIds, corporationIds, characterIds) ?: return null

        val allianceStandings = mutableMapOf<Int, Float>()
        val corporationStandings = mutableMapOf<Int, Float>()
        val characterStandings = mutableMapOf<Int, Float>()
        contacts.forEach { contact ->
            if (contact.standing == 0f) return@forEach
            when (contact.contactType) {
                ContactType.Character -> characterStandings[contact.contactId] = contact.standing
                ContactType.Corporation -> corporationStandings[contact.contactId] = contact.standing
                ContactType.Alliance -> allianceStandings[contact.contactId] = contact.standing
                ContactType.Faction -> {}
            }
        }

        return Standings(allianceStandings, corporationStandings, characterStandings)
    }

    private suspend fun getContacts(
        allianceIds: Map<Int, Int>,
        corporationIds: Map<Int, Int>,
        characterIds: List<Int>,
    ): List<Contact>? = coroutineScope {
        val allianceContactsDeferred = allianceIds.map { (allianceId, characterId) ->
            async { esiApi.getAlliancesIdContacts(characterId, allianceId) }
        }
        val corporationContactsDeferred = corporationIds.map { (corporationId, characterId) ->
            async { esiApi.getCorporationsIdContacts(characterId, corporationId) }
        }
        val characterContactsDeferred = characterIds.map { characterId ->
            async { esiApi.getCharactersIdContacts(characterId) }
        }
        val allianceContacts = allianceContactsDeferred.awaitAll()
            .flatMap { it.success ?: return@coroutineScope null }
        val corporationContacts = corporationContactsDeferred.awaitAll()
            .flatMap { it.success ?: return@coroutineScope null }
        val characterContacts = characterContactsDeferred.awaitAll()
            .flatMap { it.success ?: return@coroutineScope null }
        return@coroutineScope allianceContacts + corporationContacts + characterContacts
    }
}
