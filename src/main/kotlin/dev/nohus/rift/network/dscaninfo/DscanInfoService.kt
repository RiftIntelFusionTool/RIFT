package dev.nohus.rift.network.dscaninfo

import retrofit2.http.GET
import retrofit2.http.Path

interface DscanInfoService {

    @GET("/v/{id}?output=json")
    suspend fun getScan(
        @Path("id") scanId: String,
    ): DscanInfoScanResponse
}
