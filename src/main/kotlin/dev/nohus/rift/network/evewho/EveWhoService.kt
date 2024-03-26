package dev.nohus.rift.network.evewho

import retrofit2.http.GET
import retrofit2.http.Path

interface EveWhoService {

    @GET("allilist/{allianceId}")
    suspend fun getAlliance(@Path("allianceId") allianceId: String): AllianceResponse

    @GET("corplist/{corporationId}")
    suspend fun getCorporation(@Path("corporationId") corporationId: String): CorporationResponse

    @GET("character/{characterId}")
    suspend fun getCharacter(@Path("characterId") characterId: String): CharacterResponse
}
