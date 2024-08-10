package dev.nohus.rift.network.imageserver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Response
import retrofit2.Retrofit
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

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

    suspend fun getAllianceLogo(allianceId: Int, size: Int): BufferedImage? {
        val bytes = service.getAllianceLogo(allianceId, size).body()?.bytes() ?: return null
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(bytes))
        }
    }

    suspend fun getCorporationLogo(corporationId: Int, size: Int): BufferedImage? {
        val bytes = service.getCorporationLogo(corporationId, size).body()?.bytes() ?: return null
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(bytes))
        }
    }
}
