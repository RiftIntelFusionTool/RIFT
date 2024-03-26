package dev.nohus.rift.sso.authentication

import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.settings.persistence.SsoAuthentication
import dev.nohus.rift.sso.authentication.Authentication.EveAuthentication
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class EveSsoRepository(
    private val settings: Settings,
) {
    private val authentications = mutableMapOf<Int, EveAuthentication>()

    init {
        settings.authenticatedCharacters.forEach { (id, authentication) ->
            authentications += id to EveAuthentication(
                characterId = id,
                accessToken = authentication.accessToken,
                refreshToken = authentication.refreshToken,
                expiration = Instant.ofEpochMilli(authentication.expiration),
            )
        }
    }

    fun addAuthentication(authentication: EveAuthentication) {
        logger.info { "Added authentication for character ${authentication.characterId}" }
        authentications += authentication.characterId to authentication
        updateAuthenticatedCharacters()
    }

    fun removeAuthentication(authentication: EveAuthentication) {
        logger.info { "Removed authentication for character ${authentication.characterId}" }
        authentications -= authentication.characterId
        updateAuthenticatedCharacters()
    }

    fun removeAuthentication(characterId: Int) {
        logger.info { "Removed authentication for character $characterId" }
        authentications -= characterId
        updateAuthenticatedCharacters()
    }

    private fun updateAuthenticatedCharacters() {
        settings.authenticatedCharacters = authentications.map { (id, authentication) ->
            id to SsoAuthentication(
                accessToken = authentication.accessToken,
                refreshToken = authentication.refreshToken,
                expiration = authentication.expiration.toEpochMilli(),
            )
        }.toMap()
    }

    fun getAuthentication(characterId: Int): EveAuthentication? {
        return authentications[characterId]
    }
}
