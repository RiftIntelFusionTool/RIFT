package dev.nohus.rift.repositories

import dev.nohus.rift.database.local.Characters
import dev.nohus.rift.database.local.LocalDatabase
import dev.nohus.rift.logs.parse.CharacterNameValidator
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.repositories.CharactersRepository.CharacterState.DoesNotExist
import dev.nohus.rift.repositories.CharactersRepository.CharacterState.Exists
import dev.nohus.rift.repositories.CharactersRepository.CharacterState.Inactive
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class CharactersRepository(
    private val esiApi: EsiApi,
    private val characterActivityRepository: CharacterActivityRepository,
    private val localDatabase: LocalDatabase,
    private val characterNameValidator: CharacterNameValidator,
) {

    private data class Character(
        val name: String,
        val characterId: Int?,
        val isInactive: Boolean?,
        val exists: Boolean,
        val checkTimestamp: Long,
    )

    sealed interface CharacterState {
        data class Exists(
            val characterId: Int,
        ) : CharacterState

        data class Inactive(
            val characterId: Int,
        ) : CharacterState

        data object DoesNotExist : CharacterState
    }

    suspend fun getCharacterNamesStatus(names: List<String>): Map<String, CharacterState> {
        val databaseCharacters = getCharactersFromDatabase(names)

        val missing = names.filter { it !in databaseCharacters.keys }
        val esiCharacters: Map<String, CharacterState> = if (missing.isNotEmpty()) {
            val characters = getCharactersFromEsi(missing)
            saveCharactersToDatabase(characters)
            characters
                .filter { it.name in missing }
                .associate {
                    it.name to when {
                        it.isInactive == true -> Inactive(it.characterId!!)
                        it.exists -> Exists(it.characterId!!)
                        else -> DoesNotExist
                    }
                }
        } else {
            emptyMap()
        }

        if (logger.isDebugEnabled() && esiCharacters.isNotEmpty()) {
            logger.debug { "Checked ${esiCharacters.size} characters from ESI" }
        }

        return databaseCharacters + esiCharacters
    }

    suspend fun getCharacterId(name: String): Int? {
        if (!characterNameValidator.isValid(name)) {
            return null
        }
        return when (val status = getCharacterNamesStatus(listOf(name)).entries.single().value) {
            DoesNotExist -> null
            is Exists -> status.characterId
            is Inactive -> status.characterId
        }
    }

    private suspend fun getCharactersFromEsi(names: List<String>): List<Character> = withContext(Dispatchers.IO) {
        val now = Instant.now().toEpochMilli()
        val esiCharacters = esiApi.postUniverseIds(names).success?.characters.orEmpty()
            .map { async { it to characterActivityRepository.isActive(it.id) } }
            .awaitAll()
            .map { (character, isActive) ->
                Character(character.name, character.id, isActive?.let { !it }, true, now)
            }
        val existing = esiCharacters.map { it.name }
        val notExisting = names.filter { it !in existing }
        val notExistingCharacters = notExisting
            .map { Character(it, null, null, false, now) }
        esiCharacters + notExistingCharacters
    }

    private suspend fun getCharactersFromDatabase(names: List<String>): Map<String, CharacterState> =
        withContext(Dispatchers.IO) {
            localDatabase.transaction {
                Characters.selectAll().where { Characters.name inList names }.associate { row ->
                    row[Characters.name] to when {
                        row[Characters.isInactive] == true -> Inactive(row[Characters.characterId]!!)
                        row[Characters.exists] -> Exists(row[Characters.characterId]!!)
                        else -> DoesNotExist
                    }
                }
            }
        }

    private suspend fun saveCharactersToDatabase(characters: List<Character>) = withContext(Dispatchers.IO) {
        localDatabase.transaction {
            Characters.batchUpsert(characters) {
                this[Characters.name] = it.name
                this[Characters.characterId] = it.characterId
                this[Characters.isInactive] = it.isInactive
                this[Characters.exists] = it.exists
                this[Characters.checkTimestamp] = it.checkTimestamp
            }
        }
    }
}
