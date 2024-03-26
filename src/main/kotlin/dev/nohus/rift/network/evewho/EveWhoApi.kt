package dev.nohus.rift.network.evewho

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Retrofit

@Single
class EveWhoApi(
    json: Json,
    client: OkHttpClient,
) {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://evewho.com/api/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(EveWhoService::class.java)

    suspend fun getAlliance(allianceId: String): AllianceResponse {
        return service.getAlliance(allianceId)
    }

    suspend fun getCorporation(corporationId: String): CorporationResponse {
        return service.getCorporation(corporationId)
    }

    suspend fun getCharacter(characterId: String): CharacterResponse {
        return service.getCharacter(characterId)
    }
}
