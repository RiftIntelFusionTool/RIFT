package dev.nohus.rift.network.evescout

import retrofit2.http.GET

interface EveScoutRescueService {

    @GET("/home/stormtrack.php")
    suspend fun getStormTrack(): String
}
