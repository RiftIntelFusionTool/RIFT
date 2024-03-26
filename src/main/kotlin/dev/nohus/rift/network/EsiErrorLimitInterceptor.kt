package dev.nohus.rift.network

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}
private const val BASE_ERRORS_REMAINING = 100

@Single
class EsiErrorLimitInterceptor : Interceptor {

    private var resetTimestamp: Instant = Instant.EPOCH
    private var errorsRemaining = BASE_ERRORS_REMAINING
    private val mutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        mutex.withLock {
            if (errorsRemaining < 10) {
                logger.error { "Throttling ESI requests due to being close to the error limit" }
                delay(Duration.between(Instant.now(), resetTimestamp).toMillis())
                errorsRemaining = BASE_ERRORS_REMAINING
            }
        }

        val request = chain.request()
        val response = chain.proceed(request)

        response.header("warning")?.let {
            when (it) {
                "199" -> logger.warn { "ESI warning: There is a new version of this endpoint" }
                "299" -> logger.warn { "ESI warning: This endpoint is deprecated" }
                else -> logger.warn { "ESI warning: Unknown $it" }
            }
        }

        mutex.withLock {
            response.header("x-esi-error-limit-remain")?.toIntOrNull()?.let {
                errorsRemaining = it
            }
            response.header("x-esi-error-limit-reset")?.toLongOrNull()?.let {
                resetTimestamp = Instant.now() + Duration.ofSeconds(it)
            }
        }

        response
    }
}
