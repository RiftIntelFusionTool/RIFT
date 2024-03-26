package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.Types
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class TypesRepository(
    staticDatabase: StaticDatabase,
) {

    private val typeIds: Map<String, Int>

    init {
        val rows = staticDatabase.transaction {
            Types.selectAll().toList()
        }
        typeIds = rows.associate { it[Types.typeName] to it[Types.typeId] }
    }

    fun getTypeId(name: String): Int? {
        return typeIds[name]
    }
}
