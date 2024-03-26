package dev.nohus.rift.sso.authentication

import java.time.Instant

sealed class Authentication(
    open val accessToken: String,
    open val refreshToken: String,
    open val expiration: Instant,
) {
    data class EveAuthentication(
        val characterId: Int,
        override val accessToken: String,
        override val refreshToken: String,
        override val expiration: Instant,
    ) : Authentication(accessToken, refreshToken, expiration)
}
