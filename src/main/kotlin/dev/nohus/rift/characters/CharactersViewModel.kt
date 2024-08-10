package dev.nohus.rift.characters

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.files.CopyEveCharacterSettingsUseCase
import dev.nohus.rift.characters.repositories.CharacterWalletRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.location.CharacterLocationRepository.Location
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class CharactersViewModel(
    private val copyEveCharacterSettingsUseCase: CopyEveCharacterSettingsUseCase,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val characterWalletRepository: CharacterWalletRepository,
    private val settings: Settings,
) : ViewModel() {

    data class CharacterItem(
        val characterId: Int,
        val settingsFile: Path?,
        val isAuthenticated: Boolean,
        val isHidden: Boolean,
        val info: AsyncResource<LocalCharactersRepository.CharacterInfo>,
        val walletBalance: Double?,
    )

    data class UiState(
        val characters: List<CharacterItem> = emptyList(),
        val onlineCharacters: List<Int> = emptyList(),
        val locations: Map<Int, Location> = emptyMap(),
        val copying: CopyingState = CopyingState.NotCopying,
        val isChoosingDisabledCharacters: Boolean = false,
        val dialogMessage: DialogMessage? = null,
        val isSsoDialogOpen: Boolean = false,
    )

    sealed interface CopyingState {
        data object NotCopying : CopyingState
        data object SelectingSource : CopyingState
        data class SelectingDestination(
            val sourceId: Int,
        ) : CopyingState

        data class DestinationSelected(
            val source: CopyingCharacter,
            val destination: List<CopyingCharacter>,
        ) : CopyingState
    }

    data class CopyingCharacter(
        val id: Int,
        val name: String,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            localCharactersRepository.load()
            combine(
                localCharactersRepository.allCharacters,
                onlineCharactersRepository.onlineCharacters,
                characterWalletRepository.balances,
                settings.updateFlow,
            ) { characters, onlineCharacters, balances, _ ->
                val items = characters
                    .map { localCharacter ->
                        CharacterItem(
                            characterId = localCharacter.characterId,
                            settingsFile = localCharacter.settingsFile,
                            isAuthenticated = localCharacter.isAuthenticated,
                            isHidden = localCharacter.isHidden,
                            info = localCharacter.info,
                            walletBalance = balances[localCharacter.characterId],
                        )
                    }
                val sortedItems = items.sortedByDescending { it.characterId in onlineCharacters }
                _state.update { it.copy(characters = sortedItems) }
            }.collect()
        }
        viewModelScope.launch {
            characterLocationRepository.locations.collect { locations ->
                _state.update { it.copy(locations = locations) }
            }
        }
        observeOnlineCharacters()
    }

    fun onSsoClick() {
        _state.update { it.copy(isSsoDialogOpen = true) }
    }

    fun onCloseSso() {
        _state.update { it.copy(isSsoDialogOpen = false) }
    }

    fun onCopySettingsClick() {
        _state.update {
            it.copy(
                copying = CopyingState.SelectingSource,
                isChoosingDisabledCharacters = false,
            )
        }
    }

    fun onCopyCancel() {
        _state.update { it.copy(copying = CopyingState.NotCopying) }
    }

    fun onCopySourceClick(characterId: Int) {
        _state.update { it.copy(copying = CopyingState.SelectingDestination(characterId)) }
    }

    fun onCopyDestinationClick(characterId: Int) {
        val state = _state.value.copying
        if (state is CopyingState.SelectingDestination) {
            val sourceName =
                _state.value.characters.firstOrNull { it.characterId == state.sourceId }?.info?.success?.name ?: return
            val destinationName =
                _state.value.characters.firstOrNull { it.characterId == characterId }?.info?.success?.name ?: return
            _state.update {
                it.copy(
                    copying = CopyingState.DestinationSelected(
                        source = CopyingCharacter(state.sourceId, sourceName),
                        destination = listOf(CopyingCharacter(characterId, destinationName)),
                    ),
                )
            }
        } else if (state is CopyingState.DestinationSelected) {
            val destinationName =
                _state.value.characters.firstOrNull { it.characterId == characterId }?.info?.success?.name ?: return
            val destinations = state.destination + CopyingCharacter(characterId, destinationName)
            _state.update {
                it.copy(
                    copying = CopyingState.DestinationSelected(
                        source = state.source,
                        destination = destinations,
                    ),
                )
            }
        }
    }

    fun onCopySettingsConfirmClick() {
        val state = _state.value.copying
        if (state is CopyingState.DestinationSelected) {
            val fromFile = _state.value.characters
                .firstOrNull { it.characterId == state.source.id }?.settingsFile ?: return
            val toFiles = _state.value.characters
                .filter { it.characterId in state.destination.map { it.id } }
                .mapNotNull { it.settingsFile }
                .takeIf { it.size == state.destination.size } ?: return
            val success = copyEveCharacterSettingsUseCase(fromFile, toFiles)

            val dialogMessage = if (success) {
                DialogMessage(
                    title = "Settings copied",
                    message = "Eve settings have been copied from ${state.source.name} to ${state.destination.joinToString { it.name }}.",
                    type = MessageDialogType.Info,
                )
            } else {
                DialogMessage(
                    title = "Copying failed",
                    message = "There is something wrong with your character settings files.",
                    type = MessageDialogType.Warning,
                )
            }
            _state.update {
                it.copy(
                    copying = CopyingState.NotCopying,
                    dialogMessage = dialogMessage,
                )
            }
        }
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
    }

    fun onChooseDisabledClick() {
        _state.update {
            it.copy(
                copying = CopyingState.NotCopying,
                isChoosingDisabledCharacters = !it.isChoosingDisabledCharacters,
            )
        }
    }

    fun onDisableCharacterClick(characterId: Int) {
        settings.hiddenCharacterIds += characterId
    }

    fun onEnableCharacterClick(characterId: Int) {
        settings.hiddenCharacterIds -= characterId
    }

    private fun observeOnlineCharacters() = viewModelScope.launch {
        onlineCharactersRepository.onlineCharacters.collect { onlineCharacters ->
            _state.update { it.copy(onlineCharacters = onlineCharacters) }
        }
    }
}
