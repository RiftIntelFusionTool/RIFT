package dev.nohus.rift.network.esi

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.nohus.rift.network.RequestExecutor
import dev.nohus.rift.network.Result
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Response
import retrofit2.Retrofit

@Single
class EsiApi(
    json: Json,
    @Named("esi") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://esi.evetech.net/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(EsiService::class.java)

    suspend fun postUniverseIds(names: List<String>): Result<UniverseIdsResponse> {
        return execute { service.postUniverseIds(names) }
    }

    suspend fun postUniverseNames(ids: List<Int>): Result<List<UniverseName>> {
        return execute { service.postUniverseNames(ids) }
    }

    suspend fun getCharactersId(characterId: Int): Result<CharactersIdCharacter> {
        return execute { service.getCharactersId(characterId) }
    }

    suspend fun getCorporationsId(corporationId: Int): Result<CorporationsIdCorporation> {
        return execute { service.getCorporationsId(corporationId) }
    }

    suspend fun getAlliancesId(allianceId: Int): Result<AlliancesIdAlliance> {
        return execute { service.getAlliancesId(allianceId) }
    }

    suspend fun getAlliancesIdContacts(characterId: Int, allianceId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getAlliancesIdContacts(allianceId, authorization)
        }
    }

    suspend fun getCorporationsIdContacts(characterId: Int, corporationId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCorporationsIdContacts(corporationId, authorization)
        }
    }

    suspend fun getCharactersIdContacts(characterId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdContacts(characterId, authorization)
        }
    }

    suspend fun getCharacterIdOnline(characterId: Int): Result<CharacterIdOnline> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharacterIdOnline(characterId, authorization)
        }
    }

    suspend fun getCharacterIdLocation(characterId: Int): Result<CharacterIdLocation> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharacterIdLocation(characterId, authorization)
        }
    }

    suspend fun getCharacterIdWallet(characterId: Int): Result<Double> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdWallet(characterId, authorization)
        }
    }

    suspend fun getCharactersIdSearch(characterId: Int, categories: String, strict: Boolean, search: String): Result<CharactersIdSearch> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdSearch(characterId, categories, strict, search, authorization)
        }
    }

    suspend fun getCharactersIdClones(characterId: Int): Result<CharactersIdClones> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdClones(characterId, authorization)
        }
    }

    suspend fun getCharactersIdImplants(characterId: Int): Result<List<Int>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdImplants(characterId, authorization)
        }
    }

    suspend fun getUniverseStationsId(stationId: Int): Result<UniverseStationsId> {
        return execute { service.getUniverseStationsId(stationId) }
    }

    suspend fun getUniverseStructuresId(structureId: Long, characterId: Int): Result<UniverseStructuresId> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getUniverseStructuresId(structureId, authorization)
        }
    }

    suspend fun getUniverseSystemJumps(): Result<List<UniverseSystemJumps>> {
        return execute { service.getUniverseSystemJumps() }
    }

    suspend fun getUniverseSystemKills(): Result<List<UniverseSystemKills>> {
        return execute { service.getUniverseSystemKills() }
    }

    suspend fun getIncursions(): Result<List<Incursion>> {
        return execute { service.getIncursions() }
    }

    suspend fun getFactionWarfareSystems(): Result<List<FactionWarfareSystem>> {
        return execute { service.getFactionWarfareSystems() }
    }

    suspend fun getSovereigntyMap(): Result<List<SovereigntySystem>> {
        return execute { service.getSovereigntyMap() }
    }

    suspend fun postUiAutopilotWaypoint(
        destinationId: Long,
        clearOtherWaypoints: Boolean,
        characterId: Int,
    ): Result<Unit> {
        return executeEveAuthorized(characterId) { authorization ->
            service.postUiAutopilotWaypoint(
                addToBeginning = false,
                clearOtherWaypoints = clearOtherWaypoints,
                destinationId = destinationId,
                authorization = authorization,
            )
        }
    }

    suspend fun getCharactersIdAssets(page: Int, characterId: Int): Result<Response<List<CharactersIdAsset>>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdAssets(characterId, page, authorization)
        }
    }

    suspend fun getCharactersIdAssetsNames(characterId: Int, assets: List<Long>): Result<List<CharactersIdAssetsName>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdAssetsNames(characterId, assets, authorization)
        }
    }

    suspend fun getMarketsPrices(): Result<List<MarketsPrice>> {
        return execute { service.getMarketsPrices() }
    }

    suspend fun getCharactersIdFleet(characterId: Int): Result<CharactersIdFleet> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdFleet(characterId, authorization)
        }
    }

    suspend fun getFleetsId(characterId: Int, fleetId: Long): Result<FleetsId> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getFleetsId(fleetId, authorization)
        }
    }

    suspend fun getFleetsIdMembers(characterId: Int, fleetId: Long): Result<List<FleetMember>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getFleetsIdMembers(fleetId, authorization)
        }
    }

    suspend fun getCharactersIdPlanets(characterId: Int): Result<List<CharactersIdPlanet>> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdPlanets(characterId, authorization)
        }
    }

    suspend fun getCharactersIdPlanetsId(characterId: Int, planetId: Int): Result<CharactersIdPlanetsId> {
        return executeEveAuthorized(characterId) { authorization ->
            service.getCharactersIdPlanetsId(characterId, planetId, authorization)
        }
    }
}
