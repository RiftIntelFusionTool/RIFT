package dev.nohus.rift.repositories

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import org.koin.core.annotation.Single

@Single
class GetSystemDistanceFromCharacterUseCase(
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
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

        // Try online characters
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
        characterIds
            .mapNotNull { characterId ->
                characterLocations[characterId]?.solarSystemId ?: return@mapNotNull null
            }
            .distinct()
            .mapNotNull { characterSystemId ->
                getSystemDistanceUseCase(characterSystemId, systemId, maxDistance = maxDistance, withJumpBridges = withJumpBridges) ?: return@mapNotNull null
            }
            .minOrNull()?.let { distance ->
                return distance
            }
        return null
    }
}
