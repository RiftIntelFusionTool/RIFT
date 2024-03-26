package dev.nohus.rift.network

import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single

@Single
class UserAgentInterceptor : Interceptor {

    private val userAgentKey = "User-Agent"
    private val userAgent = "RIFT (contact: developer@riftforeve.online)"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(userAgentKey, userAgent)
            .build()
        return chain.proceed(request)
    }
}
