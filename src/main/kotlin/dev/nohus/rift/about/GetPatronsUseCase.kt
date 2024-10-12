package dev.nohus.rift.about

import dev.nohus.rift.network.HttpGetUseCase
import dev.nohus.rift.network.HttpGetUseCase.CacheBehavior
import dev.nohus.rift.network.Result
import dev.nohus.rift.repositories.CharacterDetailsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class GetPatronsUseCase(
    private val httpGet: HttpGetUseCase,
    private val characterDetailsRepository: CharacterDetailsRepository,
) {

    data class Patron(
        val characterId: Int,
        val name: String,
    )

    suspend operator fun invoke(cache: CacheBehavior): Result<List<Patron>> {
        return coroutineScope {
            val characterIds = httpGet("https://riftforeve.online/patrons.txt", cache).success
                ?.trim()?.lines()?.mapNotNull { it.toIntOrNull() } ?: return@coroutineScope Result.Failure()
            val patrons = characterIds.map { id ->
                async {
                    characterDetailsRepository.getCharacterDetails(id)
                }
            }.awaitAll().mapNotNull { response ->
                val details = response ?: return@mapNotNull null
                Patron(details.characterId, details.name)
            }
            Result.Success(patrons)
        }
    }
}
