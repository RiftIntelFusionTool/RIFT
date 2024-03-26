package dev.nohus.rift.sso.authentication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SsoErrorResponse(
    @SerialName("error")
    val error: String,
    @SerialName("error_description")
    val errorDescription: String? = null,
)
