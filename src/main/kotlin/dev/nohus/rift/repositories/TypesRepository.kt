package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.Types
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class TypesRepository(
    staticDatabase: StaticDatabase,
    private val esiApi: EsiApi,
) {

    data class Type(
        val id: Int,
        val name: String,
        val volume: Float,
        val iconId: Int,
    )

    /**
     * Names resolved from ESI for types not in the SDE
     */
    private val resolvedTypeNames = mutableMapOf<Int, String>()
    private val types: Map<Int, Type>
    private val typeIds: Map<String, Int>

    init {
        val rows = staticDatabase.transaction {
            Types.selectAll().toList()
        }
        types = rows.associate {
            it[Types.typeId] to Type(
                id = it[Types.typeId],
                name = it[Types.typeName],
                volume = it[Types.volume],
                iconId = it[Types.iconId] ?: it[Types.typeId],
            )
        }
        typeIds = rows.associate { it[Types.typeName] to it[Types.typeId] }
    }

    fun getTypeId(name: String): Int? {
        return typeIds[name]
    }

    fun getTypeName(id: Int): String? {
        return getType(id)?.name ?: resolvedTypeNames[id]
    }

    fun getType(id: Int): Type? {
        return types[id]
    }

    suspend fun resolveNamesFromEsi(ids: List<Int>) {
        resolvedTypeNames += ids
            .distinct()
            .filter { getTypeName(it) == null }
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
