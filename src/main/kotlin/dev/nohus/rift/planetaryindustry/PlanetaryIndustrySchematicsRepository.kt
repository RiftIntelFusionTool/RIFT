package dev.nohus.rift.planetaryindustry

import dev.nohus.rift.database.static.PlanetaryIndustrySchematics
import dev.nohus.rift.database.static.PlanetaryIndustrySchematicsTypes
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single
import java.time.Duration

@Single
class PlanetaryIndustrySchematicsRepository(
    private val typesRepository: TypesRepository,
    staticDatabase: StaticDatabase,
) {

    data class Schematic(
        val id: Int,
        val cycleTime: Duration,
        val outputType: Type,
        val outputQuantity: Long,
        val inputs: Map<Type, Long>,
    )

    private val schematicsById: Map<Int, Schematic>

    init {
        val typesById = staticDatabase.transaction {
            PlanetaryIndustrySchematicsTypes.selectAll().toList()
        }.groupBy { it[PlanetaryIndustrySchematicsTypes.id] }
        val schematics = staticDatabase.transaction {
            PlanetaryIndustrySchematics.selectAll().toList()
        }.map { row ->
            val id = row[PlanetaryIndustrySchematics.id]
            val cycleTime = Duration.ofSeconds(row[PlanetaryIndustrySchematics.cycleTime])
            val types = typesById[id]!!
            val (outputType, outputQuantity) = types
                .first { !it[PlanetaryIndustrySchematicsTypes.isInput] }
                .let {
                    val outputId = it[PlanetaryIndustrySchematicsTypes.typeId]
                    val outputType = typesRepository.getTypeOrPlaceholder(outputId)
                    val outputQuantity = it[PlanetaryIndustrySchematicsTypes.quantity].toLong()
                    outputType to outputQuantity
                }
            val inputs = types
                .filter { it[PlanetaryIndustrySchematicsTypes.isInput] }
                .associate {
                    val typeId = it[PlanetaryIndustrySchematicsTypes.typeId]
                    val type = typesRepository.getTypeOrPlaceholder(typeId)
                    val quantity = it[PlanetaryIndustrySchematicsTypes.quantity].toLong()
                    type to quantity
                }
            Schematic(id, cycleTime, outputType, outputQuantity, inputs)
        }
        schematicsById = schematics.associateBy { it.id }
    }

    fun getSchematic(id: Int): Schematic? {
        return schematicsById[id]
    }
}
