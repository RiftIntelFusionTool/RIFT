package dev.nohus.rift.characters.repositories

import dev.nohus.rift.network.esi.EsiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

@Single
class CharacterWalletRepository(
    private val localCharactersRepository: LocalCharactersRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val esiApi: EsiApi,
) {

    private val _balances = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val balances = _balances.asStateFlow()

    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters.collect {
                load(it.filter { it.isAuthenticated }.map { it.characterId })
            }
        }
        launch {
            while (true) {
                delay(2.minutes)
                load(onlineCharactersRepository.onlineCharacters.value)
            }
        }
    }

    private suspend fun load(characters: List<Int>) = withContext(Dispatchers.IO) {
        if (characters.isEmpty()) return@withContext
        characters.map { characterId ->
            async {
                characterId to esiApi.getCharacterIdWallet(characterId)
            }
        }.awaitAll().map { (characterId, result) ->
            result.success?.let { balance ->
                _balances.update { it + (characterId to balance) }
            }
        }
    }
}
