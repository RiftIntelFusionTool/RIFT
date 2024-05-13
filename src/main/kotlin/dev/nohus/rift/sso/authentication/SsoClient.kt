package dev.nohus.rift.sso.authentication

import dev.nohus.rift.sso.SsoAuthority
import dev.nohus.rift.utils.openBrowser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.hc.core5.net.URIBuilder
import org.jose4j.jwk.HttpsJwks
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import org.koin.core.annotation.Factory
import java.io.IOException
import java.net.URI
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

@Factory
class SsoClient(
    private val server: CallbackServer,
    private val eveSsoRepository: EveSsoRepository,
    private val json: Json,
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    data class SsoConfiguration(
        val authority: SsoAuthority,
        val callbackPort: Int,
        val callbackUrl: String,
        val authorizationEndpoint: String,
        val tokenEndpoint: String,
        val tokenRedirectUrl: String?,
        val jwksEndpoint: String,
        val jwtExpectedIssuer: String,
        val clientId: String,
        val clientSecret: String?,
        val scopes: String,
    )

    private fun getSsoConfiguration(authority: SsoAuthority) = when (authority) {
        SsoAuthority.Eve -> SsoConfiguration(
            authority = SsoAuthority.Eve,
            callbackPort = 25252,
            callbackUrl = "http://localhost:25252/callback",
            authorizationEndpoint = "https://login.eveonline.com/v2/oauth/authorize",
            tokenEndpoint = "https://login.eveonline.com/v2/oauth/token",
            tokenRedirectUrl = null,
            jwksEndpoint = "https://login.eveonline.com/oauth/jwks",
            jwtExpectedIssuer = "https://login.eveonline.com",
            clientId = "84f160b7c2d54bddb41c0ef587923e65",
            clientSecret = null,
            scopes = listOf(
                "esi-location.read_online.v1",
                "esi-location.read_location.v1",
                "esi-universe.read_structures.v1",
                "esi-ui.write_waypoint.v1",
                "esi-wallet.read_character_wallet.v1",
                "esi-search.search_structures.v1",
            ).joinToString(" "),
        )
    }

    suspend fun authenticate(authority: SsoAuthority): Authentication = withContext(Dispatchers.IO) {
        val codeVerifier = generateRandomBytes()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateRandomBytes()
        val completableDeferred = CompletableDeferred<Authentication>()
        val configuration = getSsoConfiguration(authority)

        server.start(configuration.callbackPort)
        server.setCallback { code, responseState ->
            if (responseState == state) {
                server.stop()
                try {
                    val response = postSsoTokenRequest(configuration, code, codeVerifier)
                    completableDeferred.complete(response.toAuthentication(configuration))
                } catch (e: Exception) {
                    completableDeferred.completeExceptionally(e)
                }
            }
        }

        val uri = buildSsoUrl(configuration, codeChallenge, state)
        uri.openBrowser()

        completableDeferred.await()
    }

    fun cancel() {
        server.stop()
    }

    suspend fun refreshToken(authority: SsoAuthority, authentication: Authentication): Authentication = withContext(Dispatchers.IO) {
        try {
            val configuration = getSsoConfiguration(authority)
            val response = httpClient.submitForm(
                url = configuration.tokenEndpoint,
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", authentication.refreshToken)
                    append("client_id", configuration.clientId)
                    if (configuration.clientSecret != null) {
                        append("client_secret", configuration.clientSecret)
                    }
                },
            )
            if (response.status.isSuccess()) {
                val tokenResponse: TokenResponse = response.body()
                tokenResponse.toAuthentication(configuration)
            } else {
                val errorResponse: SsoErrorResponse = response.body()
                handleSsoError(authentication, errorResponse)
                throw SsoException(errorResponse)
            }
        } catch (e: SsoException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to refresh token" }
            throw SsoException(null)
        }
    }

    private fun validateJwt(configuration: SsoConfiguration, accessToken: String): JwtClaims {
        val httpsJwks = HttpsJwks(configuration.jwksEndpoint)
        val keyResolver = HttpsJwksVerificationKeyResolver(httpsJwks)
        val jwtConsumer = JwtConsumerBuilder()
            .setVerificationKeyResolver(keyResolver)
            .setExpectedIssuer(configuration.jwtExpectedIssuer)
            .setRequireExpirationTime()
            .setExpectedAudience(configuration.clientId, "EVE Online")
            .build()
        return jwtConsumer.processToClaims(accessToken)
    }

    private fun TokenResponse.toAuthentication(configuration: SsoConfiguration): Authentication {
        val claims = validateJwt(configuration, accessToken)
        val expires = Instant.now() + Duration.ofSeconds(expiresIn.toLong() - 1)
        val refreshToken = refreshToken ?: throw IOException("No refresh token in token response")
        return when (configuration.authority) {
            SsoAuthority.Eve -> {
                val characterId = claims.subject.substringAfter("CHARACTER:EVE:").toInt()
                Authentication.EveAuthentication(characterId, accessToken, refreshToken, expires)
            }
        }
    }

    private fun buildSsoUrl(
        configuration: SsoConfiguration,
        codeChallenge: String,
        state: String,
    ): URI {
        return URIBuilder(configuration.authorizationEndpoint).apply {
            addParameter("response_type", "code")
            addParameter("redirect_uri", configuration.callbackUrl)
            addParameter("client_id", configuration.clientId)
            addParameter("scope", configuration.scopes)
            addParameter("code_challenge", codeChallenge)
            addParameter("code_challenge_method", "S256")
            addParameter("state", state)
        }.build()
    }

    private suspend fun postSsoTokenRequest(
        configuration: SsoConfiguration,
        code: String,
        codeVerifier: String,
    ): TokenResponse {
        return httpClient.submitForm(
            url = configuration.tokenEndpoint,
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("client_id", configuration.clientId)
                append("code_verifier", codeVerifier)
                if (configuration.clientSecret != null) {
                    append("client_secret", configuration.clientSecret)
                }
                if (configuration.tokenRedirectUrl != null) {
                    append("redirect_uri", configuration.tokenRedirectUrl)
                }
            },
        ).body()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateRandomBytes(): String {
        val bytes = ByteArray(32)
        Random.nextBytes(bytes)
        return Base64.UrlSafe.encode(bytes)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray())
        return Base64.UrlSafe.encode(hash).replace("=", "")
    }

    private fun handleSsoError(authentication: Authentication, errorResponse: SsoErrorResponse) {
        if (errorResponse.error == "invalid_grant") {
            logger.error { "Invalid grant: $errorResponse" }
            when (authentication) {
                is Authentication.EveAuthentication -> eveSsoRepository.removeAuthentication(authentication)
            }
        } else {
            logger.error { "Unknown SSO error: $errorResponse" }
        }
    }
}
