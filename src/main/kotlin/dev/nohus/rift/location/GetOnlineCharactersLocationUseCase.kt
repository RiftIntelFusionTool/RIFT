package dev.nohus.rift.location

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository.Location
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single

@Single
class GetOnlineCharactersLocationUseCase(
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
) {

    data class OnlineCharacterLocation(
        val id: Int,
        val name: String,
        val location: Location,
    )

    operator fun invoke() = combine(
        localCharactersRepository.characters,
        onlineCharactersRepository.onlineCharacters,
        characterLocationRepository.locations,
    ) { localCharacters, onlineCharacters, locations ->
        onlineCharacters.mapNotNull { characterId ->
            val name = localCharacters.find { it.characterId == characterId }?.info?.success?.name
                ?: return@mapNotNull null
            val location = locations[characterId] ?: return@mapNotNull null
            OnlineCharacterLocation(
                id = characterId,
                name = name,
                location = location,
            )
        }
    }
}
