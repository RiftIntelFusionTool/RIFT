package dev.nohus.rift.network

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Single
class HttpGetUseCase(
    private val okHttpClient: OkHttpClient,
) {

    enum class CacheBehavior {
        Normal, CacheOnly, NetworkOnly
    }

    suspend operator fun invoke(url: String, cache: CacheBehavior = CacheBehavior.Normal): Result<String> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .run {
                    when (cache) {
                        CacheBehavior.Normal -> this
                        CacheBehavior.CacheOnly -> cacheControl(CacheControl.FORCE_CACHE)
                        CacheBehavior.NetworkOnly -> cacheControl(CacheControl.FORCE_NETWORK)
                    }
                }
                .build()
            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body ?: run {
                        logger.error { "Failed to load: $url, no body" }
                        return@withContext Result.Failure()
                    }
                    val string = body.string()
                    Result.Success(string)
                } else {
                    logger.error { "Failed to load: $url, code ${response.code}" }
                    Result.Failure()
                }.also {
                    response.body?.close()
                }
            } catch (e: IOException) {
                logger.error { "Failed to load: $url, ${e.message}" }
                Result.Failure(e)
            }
        }
    }
}
