package dev.nohus.rift.network.imageserver

import retrofit2.Response
import retrofit2.http.HEAD
import retrofit2.http.Path

interface ImageServerService {

    @HEAD("characters/{characterId}/portrait")
    suspend fun getCharacterPortrait(@Path("characterId") characterId: Int): Response<Void>
}
