package dev.nohus.rift.network.evescout

import dev.nohus.rift.network.RequestExecutor
import dev.nohus.rift.network.Result
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

@Single
class EveScoutRescueApi(
    client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://evescoutrescue.com/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    private val service = retrofit.create(EveScoutRescueService::class.java)

    suspend fun getStormTrack(): Result<String> {
        return execute { service.getStormTrack() }
    }
}
