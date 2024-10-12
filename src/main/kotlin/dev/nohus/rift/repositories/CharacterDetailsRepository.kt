package dev.nohus.rift.repositories

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.AlliancesIdAlliance
import dev.nohus.rift.network.esi.CorporationsIdCorporation
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class CharacterDetailsRepository(
    private val esiApi: EsiApi,
    private val standingsRepository: StandingsRepository,
) {

    data class CharacterDetails(
        val characterId: Int,
        val name: String,
        val corporationId: Int,
        val corporationName: String?,
        val corporationTicker: String?,
        val allianceId: Int?,
        val allianceName: String?,
        val allianceTicker: String?,
        val standing: Standing,
        val title: String?,
    )

    suspend fun getCharacterDetails(characterId: Int): CharacterDetails? = coroutineScope {
        val character = esiApi.getCharactersId(characterId).success ?: return@coroutineScope null
        val deferredCorporation = async { esiApi.getCorporationsId(character.corporationId).success }
        val deferredAlliance = async { character.allianceId?.let { esiApi.getAlliancesId(it).success } }
        val corporation = deferredCorporation.await()
        val alliance = deferredAlliance.await()
        val standing = standingsRepository.getStanding(character.allianceId, character.corporationId, characterId)
        CharacterDetails(
            characterId = characterId,
            name = character.name,
            corporationId = character.corporationId,
            corporationName = corporation?.name,
            corporationTicker = corporation?.ticker,
            allianceId = character.allianceId,
            allianceName = alliance?.name,
            allianceTicker = alliance?.ticker,
            standing = standing,
            title = character.title,
        )
    }

    suspend fun getCorporationName(corporationId: Int): Result<CorporationsIdCorporation> {
        return esiApi.getCorporationsId(corporationId)
    }

    suspend fun getAllianceName(allianceId: Int): Result<AlliancesIdAlliance> {
        return esiApi.getAlliancesId(allianceId)
    }
}
