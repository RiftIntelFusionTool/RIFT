package dev.nohus.rift.map

import dev.nohus.rift.database.static.MapLayout
import dev.nohus.rift.database.static.RegionMapLayout
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single
import kotlin.math.roundToInt

@Single
class MapLayoutRepository(
    staticDatabase: StaticDatabase,
    val solarSystemsRepository: SolarSystemsRepository,
) {

    data class Position(
        val x: Int,
        val y: Int,
    )

    // region ID -> system ID -> position
    private val systemPositionsByRegionId: Map<Int, Map<Int, Position>>

    // region ID -> position
    private val regionPositionsById: Map<Int, Position>

    init {
        val rows = staticDatabase.transaction {
            MapLayout.selectAll().toList()
        }
        systemPositionsByRegionId = rows
            .groupBy { it[MapLayout.regionId] }
            .mapValues { (_, rows) ->
                rows.associate {
                    it[MapLayout.solarSystemId] to Position(it[MapLayout.x], it[MapLayout.y])
                }
            }
        val regionRows = staticDatabase.transaction {
            RegionMapLayout.selectAll().toList()
        }
        regionPositionsById = regionRows.associate {
            it[RegionMapLayout.regionId] to Position(it[RegionMapLayout.x], it[RegionMapLayout.y])
        }
    }

    fun getLayout(regionId: Int): Map<Int, Position> {
        return systemPositionsByRegionId[regionId] ?: throw IllegalArgumentException("No such region: $regionId")
    }

    fun getNewEdenLayout(): Map<Int, Position> {
        return solarSystemsRepository.mapSolarSystems.associate { system ->
            system.id to transformNewEdenCoordinate(system.x, system.z)
        }
    }

    fun getRegionLayout(): Map<Int, Position> {
        return regionPositionsById
    }

    companion object {
        /**
         * Maps in-game system coordinates to map layout coordinates with reasonable values
         */
        fun transformNewEdenCoordinate(x: Double, z: Double): Position {
            val scale = 100_000_000_000_000
            val shiftX = 5087
            val shiftY = 4729
            return Position((x / scale).roundToInt() + shiftX, (z / scale).roundToInt() + shiftY)
        }
    }
}
