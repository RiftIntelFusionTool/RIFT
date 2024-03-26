package dev.nohus.rift.network.adashboardinfo

import retrofit2.http.GET
import retrofit2.http.Path

interface AdashboardInfoService {

    @GET("/intel/dscan/view/{id}")
    suspend fun getScan(
        @Path("id") scanId: String,
    ): String
}
