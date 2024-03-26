package dev.nohus.rift.repositories

import dev.nohus.rift.network.imageserver.ImageServerApi
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Single
class CharacterActivityRepository(
    private val imageServerApi: ImageServerApi,
) {

    private var emptyPortraitEtag: String? = null

    suspend fun isActive(characterId: Int): Boolean? {
        val emptyEtag = getEmptyPortraitEtag() ?: return null
        val etag = getPortraitEtag(characterId) ?: return null
        return etag != emptyEtag
    }

    private suspend fun getEmptyPortraitEtag(): String? {
        var emptyEtag = emptyPortraitEtag
        if (emptyEtag == null) {
            emptyEtag = getPortraitEtag(1).also { emptyPortraitEtag = it }
        }
        return emptyEtag
    }

    private suspend fun getPortraitEtag(characterId: Int): String? {
        try {
            val response = imageServerApi.getCharacterPortrait(characterId)
            return response.takeIf { it.isSuccessful }?.headers()?.get("etag")
        } catch (e: IOException) {
            logger.error(e) { "Unable to get portrait etag" }
            return null
        }
    }
}
