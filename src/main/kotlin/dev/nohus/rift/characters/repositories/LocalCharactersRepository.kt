package dev.nohus.rift.characters.repositories

import dev.nohus.rift.characters.files.GetEveCharactersSettingsUseCase
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.toResource
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.stateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.nameWithoutExtension

@Single
class LocalCharactersRepository(
    private val settings: Settings,
    private val getEveCharactersSettingsUseCase: GetEveCharactersSettingsUseCase,
    private val esiApi: EsiApi,
) {

    data class LocalCharacter(
        val characterId: Int,
        val settingsFile: Path?,
        val isAuthenticated: Boolean,
        val info: AsyncResource<CharacterInfo>,
        val isHidden: Boolean,
    )

    data class CharacterInfo(
        val name: String,
        val corporationId: Int,
        val corporationName: String,
        val allianceId: Int?,
        val allianceName: String?,
    )

    private val _characters = MutableStateFlow<List<LocalCharacter>>(emptyList())
    val characters = stateFlow(
        getValue = { _characters.value.filterNot(LocalCharacter::isHidden) },
        flow = _characters.map { it.filterNot(LocalCharacter::isHidden) },
    )

    /**
     * Includes hidden characters
     */
    val allCharacters = _characters.asStateFlow()

    /**
     * Watch settings, in case the list of authenticated characters changes.
     * Then update the local characters to reflect the authentication status, or remove if a local character
     * was authenticated-only (no local settings file)
     */
    suspend fun start() {
        settings.updateFlow.collect { model ->
            val authenticatedIds = model.authenticatedCharacters.map { it.key }
            val hiddenCharacterIds = model.hiddenCharacterIds.toSet()
            _characters.value = _characters.value.mapNotNull { character ->
                val newCharacter = character.copy(
                    isAuthenticated = character.characterId in authenticatedIds,
                    isHidden = character.characterId in hiddenCharacterIds,
                )
                if (!newCharacter.isAuthenticated && newCharacter.settingsFile == null) return@mapNotNull null
                newCharacter
            }
        }
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        loadLocalCharacters()
        loadEsiCharacters()
    }

    private fun loadLocalCharacters() {
        val directory = settings.eveSettingsDirectory
        val authenticatedCharacterIds = settings.authenticatedCharacters.keys
        val hiddenCharacterIds = settings.hiddenCharacterIds.toSet()
        val charactersFromFiles = if (directory != null) {
            getEveCharactersSettingsUseCase(directory)
                .mapNotNull { file ->
                    val characterId =
                        file.nameWithoutExtension.substringAfterLast("_").toIntOrNull() ?: return@mapNotNull null
                    LocalCharacter(
                        characterId = characterId,
                        settingsFile = file,
                        isAuthenticated = characterId in authenticatedCharacterIds,
                        info = AsyncResource.Loading,
                        isHidden = characterId in hiddenCharacterIds,
                    )
                }
        } else {
            emptyList()
        }
        val characterIds = charactersFromFiles.map { it.characterId }

        val ssoOnlyCharacters = authenticatedCharacterIds
            .filter { it !in characterIds }
            .map { characterId ->
                LocalCharacter(
                    characterId = characterId,
                    settingsFile = null,
                    isAuthenticated = true,
                    info = AsyncResource.Loading,
                    isHidden = characterId in hiddenCharacterIds,
                )
            }

        _characters.value = (charactersFromFiles + ssoOnlyCharacters)
            .distinctBy { it.characterId }
            .sortedWith(
                compareBy(
                    { !it.isAuthenticated },
                    { it.settingsFile?.getLastModifiedTime()?.toMillis()?.let { -it } ?: 0L },
                ),
            )
    }

    private suspend fun loadEsiCharacters() = coroutineScope {
        for (item in _characters.value) {
            launch {
                val result = esiApi.getCharactersId(item.characterId).map { character ->
                    val corporationDeferred = async { esiApi.getCorporationsId(character.corporationId) }
                    val allianceDeferred =
                        if (character.allianceId != null) async { esiApi.getAlliancesId(character.allianceId) } else null
                    val corporation = corporationDeferred.await()
                    val alliance = allianceDeferred?.await()
                    CharacterInfo(
                        name = character.name,
                        corporationId = character.corporationId,
                        corporationName = corporation.success?.name ?: "?",
                        allianceId = character.allianceId,
                        allianceName = if (alliance != null) alliance.success?.name ?: "?" else null,
                    )
                }
                withContext(MainUIDispatcher) {
                    val characters = _characters.value.map { current ->
                        if (current.characterId == item.characterId) current.copy(info = result.toResource()) else current
                    }
                    _characters.value = characters
                }
            }
        }
    }
}
