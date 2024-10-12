package dev.nohus.rift.network.pushover

import retrofit2.http.Body
import retrofit2.http.POST

interface PushoverService {

    @POST("/1/messages.json")
    suspend fun postMessages(
        @Body message: Messages,
    ): MessagesResponse
}
