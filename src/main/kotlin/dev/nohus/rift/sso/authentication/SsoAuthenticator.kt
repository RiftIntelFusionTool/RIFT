package dev.nohus.rift.sso.authentication

import dev.nohus.rift.sso.SsoAuthority
import dev.nohus.rift.sso.authentication.Authentication.EveAuthentication
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Factory
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Factory
class SsoAuthenticator(
    private val ssoClient: SsoClient,
    private val eveSsoRepository: EveSsoRepository,
) {

    /**
     * Starts the SSO flow, redirecting the user to the SSO login page.
     * Returns once the authentication flow has finished, or failed
     */
    suspend fun authenticate(authority: SsoAuthority) {
        val authentication = ssoClient.authenticate(authority)
        when (authority) {
            SsoAuthority.Eve -> eveSsoRepository.addAuthentication(authentication as EveAuthentication)
        }
        logger.info { "SSO authentication successful ($authority)" }
    }

    /**
     * Cancels an in progress SSO flow, if any
     */
    fun cancel() {
        ssoClient.cancel()
    }

    /**
     * Retrieves an access token, refreshing it first if needed, or null if there isn't one
     */
    suspend fun getValidEveAccessToken(characterId: Int): String {
        val authentication = eveSsoRepository.getAuthentication(characterId) ?: throw NoAuthenticationException(characterId)
        return if (authentication.expiration.isBefore(Instant.now())) {
            val newAuthentication = ssoClient.refreshToken(SsoAuthority.Eve, authentication) as EveAuthentication
            logger.info { "Eve SSO access token refreshed" }
            eveSsoRepository.addAuthentication(newAuthentication)
            newAuthentication.accessToken
        } else {
            authentication.accessToken
        }
    }
}
