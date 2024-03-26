package dev.nohus.rift.network

import okhttp3.OkHttpClient
import org.koin.core.annotation.Single

@Single
class GetOkHttpUseCase(
    private val userAgentInterceptor: UserAgentInterceptor,
    private val esiErrorLimitInterceptor: EsiErrorLimitInterceptor,
    private val loggingInterceptor: LoggingInterceptor,
) {
    operator fun invoke(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(esiErrorLimitInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
}
