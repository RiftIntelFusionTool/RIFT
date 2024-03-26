package dev.nohus.rift.network.imageserver

import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Response
import retrofit2.Retrofit

@Single
class ImageServerApi(
    client: OkHttpClient,
) {

    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://images.evetech.net")
        .build()
    private val service = retrofit.create(ImageServerService::class.java)

    suspend fun getCharacterPortrait(characterId: Int): Response<Void> {
        return service.getCharacterPortrait(characterId)
    }
}
