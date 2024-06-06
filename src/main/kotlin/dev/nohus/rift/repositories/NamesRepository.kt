package dev.nohus.rift.repositories

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import org.koin.core.annotation.Single

@Single
class NamesRepository(
    private val esiApi: EsiApi,
) {

    private val names = mutableMapOf<Int, String>()

    /**
     * Returns an already known name for the ID
     */
    fun getName(id: Int): String? {
        return names[id]
    }

    suspend fun resolveNames(ids: List<Int>) {
        @Suppress("ConvertCallChainIntoSequence")
        names += ids
            .distinct()
            .filter { it !in names }
            .chunked(1000)
            .flatMap { typeIds ->
                when (val result = esiApi.postUniverseNames(typeIds)) {
                    is Result.Success -> result.data
                    is Result.Failure -> emptyList()
                }
            }
            .associate { it.id to it.name }
    }
}
