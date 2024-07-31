package dev.nohus.rift.repositories

import dev.nohus.rift.characters.repositories.ActiveCharacterRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import org.koin.core.annotation.Single

@Single
class GetSystemDistanceFromCharacterUseCase(
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
    private val activeCharacterRepository: ActiveCharacterRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
) {

    operator fun invoke(
        systemId: Int,
        maxDistance: Int,
        withJumpBridges: Boolean,
    ): Int {
        val characterLocations = characterLocationRepository.locations.value

        // Try to get distance to the active character
        val activeCharacter = activeCharacterRepository.activeCharacter.value
        getClosestDistance(systemId, listOfNotNull(activeCharacter), characterLocations, maxDistance, withJumpBridges)?.let { return it }

        // No active character or it has no known location, try online characters
        val onlineCharacters = onlineCharactersRepository.onlineCharacters.value
        getClosestDistance(systemId, onlineCharacters, characterLocations, maxDistance, withJumpBridges)?.let { return it }

        // No online characters or none had a known location, try any characters
        val characters = localCharactersRepository.characters.value.map { it.characterId }
        getClosestDistance(systemId, characters, characterLocations, maxDistance, withJumpBridges)?.let { return it }

        return Int.MAX_VALUE
    }

    private fun getClosestDistance(
        systemId: Int,
        characterIds: List<Int>,
        characterLocations: Map<Int, CharacterLocationRepository.Location>,
        maxDistance: Int,
        withJumpBridges: Boolean,
    ): Int? {
        if (characterIds.isEmpty()) return null
        characterIds.mapNotNull { characterId ->
            val characterSystemId = characterLocations[characterId]?.solarSystemId ?: return@mapNotNull null
            getSystemDistanceUseCase(characterSystemId, systemId, maxDistance = maxDistance, withJumpBridges = withJumpBridges)
        }.minOrNull()?.let { distance ->
            return distance
        }
        return null
    }
}
