package dev.nohus.rift.network.imageserver

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Path
import retrofit2.http.Query

interface ImageServerService {

    @HEAD("characters/{characterId}/portrait")
    suspend fun getCharacterPortrait(@Path("characterId") characterId: Int): Response<Void>

    @GET("alliances/{allianceId}/logo")
    suspend fun getAllianceLogo(@Path("allianceId") allianceId: Int, @Query("size") size: Int): Response<ResponseBody>

    @GET("corporations/{corporationId}/logo")
    suspend fun getCorporationLogo(@Path("corporationId") corporationId: Int, @Query("size") size: Int): Response<ResponseBody>
}
