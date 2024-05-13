package dev.nohus.rift.network

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class LoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        logger.debug { "Request --> ${request.url}" }
        try {
            val response = chain.proceed(request)
            logger.debug { "Response <== [${response.code}] ${request.url}" }
            return@runBlocking response
        } catch (e: Exception) {
            logger.info { "Failure <== ${request.url}" }
            throw e
        }
    }
}
