package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.Stations
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class StationsRepository(
    staticDatabase: StaticDatabase,
) {

    data class Station(
        val id: Int,
        val typeId: Int,
        val name: String,
    )

    private val stations: Map<Int, List<Station>>

    init {
        val rows = staticDatabase.transaction {
            Stations.selectAll().toList()
        }
        stations = rows.groupBy {
            it[Stations.systemId]
        }.map { (systemId, stations) ->
            systemId to stations.map {
                Station(
                    id = it[Stations.id],
                    typeId = it[Stations.typeId],
                    name = it[Stations.name],
                )
            }
        }.toMap()
    }

    fun getStations(): Map<Int, List<Station>> {
        return stations
    }
}
