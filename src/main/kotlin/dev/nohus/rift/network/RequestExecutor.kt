package dev.nohus.rift.network

import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiErrorResponse
import dev.nohus.rift.sso.authentication.EveSsoRepository
import dev.nohus.rift.sso.authentication.NoAuthenticationException
import dev.nohus.rift.sso.authentication.SsoAuthenticator
import dev.nohus.rift.sso.authentication.SsoException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val logger = KotlinLogging.logger {}

interface RequestExecutor {
    suspend fun <R : Any> execute(request: suspend () -> R): Result<R>
    suspend fun <R : Any> executeEveAuthorized(characterId: Int, request: suspend (authentication: String) -> R): Result<R>
}

class RequestExecutorImpl(
    private val ssoAuthenticator: SsoAuthenticator,
    private val eveSsoRepository: EveSsoRepository,
    private val json: Json,
) : RequestExecutor {

    override suspend fun <R : Any> execute(
        request: suspend () -> R,
    ): Result<R> {
        return try {
            Success(withContext(Dispatchers.IO) { request() })
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override suspend fun <R : Any> executeEveAuthorized(
        characterId: Int,
        request: suspend (authorization: String) -> R,
    ): Result<R> {
        return try {
            val accessToken = ssoAuthenticator.getValidEveAccessToken(characterId)
            Success(withContext(Dispatchers.IO) { request("Bearer $accessToken") })
        } catch (e: Exception) {
            handleError(e, characterId)
        }
    }

    private fun handleError(e: Exception, characterId: Int? = null): Failure {
        if (e is SsoException) {
            logger.error { "Could not execute request due to SSO failure: $e" }
        } else if (e is NoAuthenticationException) {
            logger.error { "Could not execute request because the character ${e.characterId} is not authenticated" }
        } else if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                try {
                    val errorResponse: EsiErrorResponse = json.decodeFromString(body)
                    if ("token is not valid" in errorResponse.error) {
                        if (characterId != null) {
                            logger.error { "Character $characterId has an invalid token, removing" }
                            eveSsoRepository.removeAuthentication(characterId)
                        }
                    } else if ("Character has been deleted" in errorResponse.error) {
                        if (characterId != null) {
                            logger.error { "Character $characterId has been deleted from the game, removing" }
                            eveSsoRepository.removeAuthentication(characterId)
                        }
                    } else if ("Forbidden" == errorResponse.error) {
                        logger.debug { "Forbidden API response" }
                    } else {
                        logger.error { "Unknown API error response: $errorResponse" }
                    }
                } catch (ignored: SerializationException) {
                    logger.error { "API HTTP error: ${e.code()} (with unknown body)" }
                }
            } else {
                logger.error { "API HTTP error: ${e.code()}" }
            }
        } else if (e is IOException) {
            logger.error { "Could not execute request: $e" }
        } else if (e is SerializationException) {
            logger.error(e) { "Unexpected API response" }
        } else if (e is CancellationException) {
            // Normal behavior, operation cancelled
        } else {
            logger.error(e) { "Unknown API Error" }
        }
        return Failure(e)
    }
}
