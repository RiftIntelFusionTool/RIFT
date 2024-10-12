package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Planets
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.planet_barren
import dev.nohus.rift.generated.resources.planet_gas
import dev.nohus.rift.generated.resources.planet_ice
import dev.nohus.rift.generated.resources.planet_lava
import dev.nohus.rift.generated.resources.planet_ocean
import dev.nohus.rift.generated.resources.planet_plasma
import dev.nohus.rift.generated.resources.planet_storm
import dev.nohus.rift.generated.resources.planet_temperate
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class PlanetsRepository(
    staticDatabase: StaticDatabase,
) {

    data class Planet(
        val id: Int,
        val systemId: Int,
        val type: PlanetType,
        val name: String,
        val radius: Float,
    )

    private val planetsBySystemId: Map<Int, List<Planet>>
    private val planetsById: Map<Int, Planet>

    init {
        val typesById = PlanetTypes.types.associateBy { it.typeId }
        val planets = staticDatabase.transaction {
            Planets.selectAll().toList()
        }.map {
            Planet(
                id = it[Planets.id],
                systemId = it[Planets.systemId],
                type = typesById[it[Planets.typeId]]!!,
                name = it[Planets.name],
                radius = it[Planets.radius],
            )
        }
        planetsBySystemId = planets.groupBy { it.systemId }
        planetsById = planets.associateBy { it.id }
    }

    fun getPlanets(): Map<Int, List<Planet>> {
        return planetsBySystemId
    }

    fun getPlanetById(id: Int): Planet? {
        return planetsById[id]
    }
}

object PlanetTypes {

    data class PlanetType(
        val typeId: Int,
        val name: String,
        val icon: DrawableResource,
    )

    val types = listOf(
        PlanetType(11, "Temperate", Res.drawable.planet_temperate),
        PlanetType(12, "Ice", Res.drawable.planet_ice),
        PlanetType(13, "Gas", Res.drawable.planet_gas),
        PlanetType(2014, "Oceanic", Res.drawable.planet_ocean),
        PlanetType(2015, "Lava", Res.drawable.planet_lava),
        PlanetType(2016, "Barren", Res.drawable.planet_barren),
        PlanetType(2017, "Storm", Res.drawable.planet_storm),
        PlanetType(2063, "Plasma", Res.drawable.planet_plasma),

        PlanetType(30889, "Shattered", Res.drawable.planet_lava),
        PlanetType(73911, "Scorched Barren", Res.drawable.planet_barren),
    )
}
