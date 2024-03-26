package dev.nohus.rift.sso.authentication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_token") // Returned by EVE SSO
    val refreshToken: String? = null,
    @SerialName("token_type")
    val tokenType: String,
)
