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

    suspend fun getCharacterIdOnline(characterId: Int): Result<CharacterIdOnline> {
        return executeEveAuthorized(characterId) { authentication ->
            service.getCharacterIdOnline(characterId, authentication)
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
        return executeEveAuthorized(characterId) { authentication ->
            service.getCharactersIdSearch(characterId, categories, strict, search, authentication)
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
        return executeEveAuthorized(characterId) { authentication ->
            service.getCharactersIdAssets(characterId, page, authentication)
        }
    }

    suspend fun getCharactersIdAssetsNames(characterId: Int, assets: List<Long>): Result<List<CharactersIdAssetsName>> {
        return executeEveAuthorized(characterId) { authentication ->
            service.getCharactersIdAssetsNames(characterId, assets, authentication)
        }
    }
}
