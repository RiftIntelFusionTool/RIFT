package dev.nohus.rift.network.adashboardinfo

import dev.nohus.rift.network.RequestExecutor
import dev.nohus.rift.network.Result
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

@Single
class AdashboardInfoApi(
    client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://adashboard.info/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    private val service = retrofit.create(AdashboardInfoService::class.java)

    suspend fun getScan(scanId: String): Result<String> {
        return execute { service.getScan(scanId) }
    }
}
