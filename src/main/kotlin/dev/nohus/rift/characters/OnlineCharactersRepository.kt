package dev.nohus.rift.characters

import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.utils.openwindows.GetOpenEveClientsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class OnlineCharactersRepository(
    private val getOpenEveClientsUseCase: GetOpenEveClientsUseCase,
    private val localCharactersRepository: LocalCharactersRepository,
    private val esiApi: EsiApi,
) {

    private val _onlineCharacters = MutableStateFlow<List<Int>>(emptyList())
    val onlineCharacters = _onlineCharacters.asStateFlow()

    private val lastSeen = mutableMapOf<Int, Instant>()
    private val onlineSinceSeenDuration = Duration.ofMinutes(1)
    private var onlineGameClients = setOf<Int>()

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                updateOnlineCharacters()
                delay(1000)
            }
        }
        launch {
            // Check online status of offline characters every 5 minutes
            val offlineOnlinePollDuration = Duration.ofMinutes(5)
            while (true) {
                delay(offlineOnlinePollDuration.toMillis())
                checkEsiOnlineCharacters(checkOnline = false)
            }
        }
        launch {
            while (true) {
                delay(5000)
                checkOpenEveClients()
            }
        }
        launch {
            // Check online status of online characters every 20 seconds
            // This will only check the ones for which we cannot see the game client
            val onlineOnlinePollDuration = Duration.ofSeconds(20)
            while (true) {
                delay(onlineOnlinePollDuration.toMillis())
                checkEsiOnlineCharacters(checkOnline = true)
            }
        }
        launch {
            @OptIn(FlowPreview::class)
            localCharactersRepository.characters.debounce(500).collect {
                checkOpenEveClients()
                checkEsiOnlineCharacters(checkOnline = false)
                checkEsiOnlineCharacters(checkOnline = true)
            }
        }
    }

    fun onCharacterLogin(characterId: Int) {
        lastSeen[characterId] = Instant.now()
        updateOnlineCharacters()
    }

    private fun updateOnlineCharacters() {
        val now = Instant.now()
        val onlineIds = lastSeen.toMap()
            .filter { (_, timestamp) -> Duration.between(timestamp, now) < onlineSinceSeenDuration }
            .map { (id, _) -> id }
        _onlineCharacters.value = onlineIds
    }

    private suspend fun checkOpenEveClients() = withContext(Dispatchers.IO) {
        val characterNames = getOpenEveClientsUseCase() ?: return@withContext
        var characterIds: Set<Int> = emptySet()
        if (characterNames.isNotEmpty()) {
            val now = Instant.now()
            characterIds = localCharactersRepository.characters.value
                .filter { it.info.success?.name in characterNames }
                .map { it.characterId }
                .toSet()
            characterIds.forEach { characterId ->
                lastSeen[characterId] = now
            }
        }
        val wentOffline = onlineGameClients - characterIds
        wentOffline.forEach {
            lastSeen.remove(it)
        }
        onlineGameClients = characterIds
        updateOnlineCharacters()
    }

    /**
     * @param checkOnline Whether to check only online, or only offline characters
     */
    private suspend fun checkEsiOnlineCharacters(checkOnline: Boolean) {
        coroutineScope {
            val now = Instant.now()
            localCharactersRepository.characters.value
                .filter { it.characterId in _onlineCharacters.value == checkOnline }
                .filter {
                    // No need to check online if we see the client
                    !checkOnline || it.characterId !in onlineGameClients
                }
                .filter { it.isAuthenticated } // No point checking unauthenticated characters
                .map {
                    async {
                        it.characterId to esiApi.getCharacterIdOnline(it.characterId)
                    }
                }.awaitAll().forEach { (characterId, result) ->
                    if (result.success?.isOnline == true) {
                        lastSeen[characterId] = now
                    }
                }
        }
        updateOnlineCharacters()
    }
}
