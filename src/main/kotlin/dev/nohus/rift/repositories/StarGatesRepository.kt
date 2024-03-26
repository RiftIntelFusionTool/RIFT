package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StarGates
import dev.nohus.rift.database.static.StaticDatabase
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single(createdAtStart = true)
class StarGatesRepository(
    staticDatabase: StaticDatabase,
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    // { fromSystem -> { toSystem -> typeId } }
    private val stargateTypeIds: Map<Int, Map<Int, Int>>
    val connections: List<Pair<Int, Int>>

    init {
        val rows = staticDatabase.transaction {
            StarGates.selectAll().toList()
        }
        stargateTypeIds = rows
            .groupBy { it[StarGates.fromSystemId] }
            .mapValues { (_, rows) ->
                rows
                    .groupBy { it[StarGates.toSystemId] }
                    .mapValues { (_, rows) ->
                        rows.single()[StarGates.starGateTypeId]
                    }
            }
        connections = rows.map {
            it[StarGates.fromSystemId] to it[StarGates.toSystemId]
        }
    }

    fun getStargateTypeId(fromSystem: String, toSystem: String): Int? {
        val fromSystemId = solarSystemsRepository.getSystemId(fromSystem) ?: return null
        val toSystemId = solarSystemsRepository.getSystemId(toSystem) ?: return null
        return getStargateTypeId(fromSystemId, toSystemId)
    }

    private fun getStargateTypeId(fromSystemId: Int, toSystemId: Int): Int? {
        return stargateTypeIds[fromSystemId]?.get(toSystemId)
    }
}
