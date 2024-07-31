package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Constellations
import dev.nohus.rift.database.static.Regions
import dev.nohus.rift.database.static.SolarSystems
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.utils.roundSecurity
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class SolarSystemsRepository(
    staticDatabase: StaticDatabase,
) {

    private val systemNames: Set<String>
    private val lowercaseSystemNames: Map<String, String>
    private val shortened4: Map<String, List<String>>
    private val shortened3: Map<String, List<String>>
    private val shortened2: Map<String, List<String>>
    private val sunTypes: Map<String, Int>
    private val systemIdsByName: Map<String, Int>
    private val systemNamesById: Map<Int, String>
    private val systemsById: Map<Int, MapSolarSystem>
    private val mapSolarSystems: List<MapSolarSystem>
    val mapConstellations: List<MapConstellation>
    val mapRegions: List<MapRegion>
    private val mapSystemConstellation: Map<Int, Int> // System ID -> Constellation ID
    private val mapConstellationSystems: Map<Int, List<Int>> // Constellation ID -> System IDs
    private val regionNamesById: Map<Int, String> // Region ID -> Region name
    private val regionNamesBySystemName: Map<String, String> // System name -> Region name
    private val regionIdBySystemId: Map<Int, Int> // System ID -> Region ID

    companion object {
        private const val DEFAULT_SUN_TYPE = 8
    }

    data class MapSolarSystem(
        val id: Int,
        val name: String,
        val constellationId: Int,
        val regionId: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val security: Double,
        val sunTypeId: Int,
    )

    data class MapConstellation(
        val id: Int,
        val name: String,
        val x: Double,
        val y: Double,
        val z: Double,
    )

    data class MapRegion(
        val id: Int,
        val name: String,
        val x: Double,
        val y: Double,
        val z: Double,
    )

    init {
        val systemRows = staticDatabase.transaction {
            SolarSystems.selectAll().toList()
        }
        val constellationRows = staticDatabase.transaction {
            Constellations.selectAll().toList()
        }
        val regionRows = staticDatabase.transaction {
            Regions.selectAll().toList()
        }
        systemNames = systemRows.map { it[SolarSystems.solarSystemName] }.toSet()
        sunTypes = systemRows.associate { it[SolarSystems.solarSystemName] to it[SolarSystems.sunTypeId] }
        systemIdsByName = systemRows.associate { it[SolarSystems.solarSystemName] to it[SolarSystems.solarSystemId] }
        systemNamesById = systemIdsByName.map { (name, id) -> id to name }.toMap()
        lowercaseSystemNames = systemNames.associateBy { it.lowercase() }
        val codeNames = systemNames.filter { "-" in it }
        shortened4 = codeNames.groupBy { it.take(4).lowercase() }
        shortened3 = codeNames.groupBy { it.take(3).lowercase() }
            .filterKeys { it !in listOf("frt", "ooo") }
        shortened2 = codeNames.groupBy { it.take(2).lowercase() }
            .filterKeys { it.count { it.isDigit() } == 1 } // We only allow 2 character abbreviations if they include a digit
            .filterKeys { it !in listOf("t1", "t2") } // No tech level names
        mapSolarSystems = systemRows.map {
            MapSolarSystem(
                id = it[SolarSystems.solarSystemId],
                name = it[SolarSystems.solarSystemName],
                constellationId = it[SolarSystems.constellationId],
                regionId = it[SolarSystems.regionId],
                x = it[SolarSystems.x],
                y = it[SolarSystems.y],
                z = -it[SolarSystems.z],
                security = it[SolarSystems.security],
                sunTypeId = it[SolarSystems.sunTypeId],
            )
        }
        systemsById = mapSolarSystems.associateBy { it.id }
        mapConstellations = constellationRows.map {
            MapConstellation(
                id = it[Constellations.constellationId],
                name = it[Constellations.constellationName],
                x = it[Constellations.x],
                y = it[Constellations.y],
                z = -it[Constellations.z],
            )
        }
        mapRegions = regionRows.map {
            MapRegion(
                id = it[Regions.regionId],
                name = it[Regions.regionName],
                x = it[Regions.x],
                y = it[Regions.y],
                z = -it[Regions.z],
            )
        }
        mapSystemConstellation = systemRows.associate {
            it[SolarSystems.solarSystemId] to it[SolarSystems.constellationId]
        }
        mapConstellationSystems = systemRows.groupBy {
            it[SolarSystems.constellationId]
        }.entries.associate { (constellationId, rows) ->
            constellationId to rows.map { it[SolarSystems.solarSystemId] }
        }
        regionNamesById = mapRegions.associate { it.id to it.name }
        regionNamesBySystemName = mapSolarSystems.associate { it.name to regionNamesById[it.regionId]!! }
        regionIdBySystemId = mapSolarSystems.associate { it.id to it.regionId }
    }

    fun getSystems(knownSpace: Boolean = true): List<MapSolarSystem> {
        return if (knownSpace) {
            mapSolarSystems.filter { it.regionId <= 10001000 } // K-space
        } else {
            mapSolarSystems
        }
    }

    /**
     * @param name Potential system name
     * @param regionHint Region the system is expected to be in, to prioritise ambiguous names
     * @param systemHints System IDs to prioritise for ambiguous names
     * @return Full name of the system or null if it's not a system name
     */
    fun getSystemName(
        name: String,
        regionHint: String?,
        systemHints: List<Int> = emptyList(),
    ): String? {
        getSystemWithoutTypos(name, regionHint, systemHints)?.let { return it }
        if ('0' in name) getSystemWithoutTypos(name.replace('0', 'O'), regionHint, systemHints)?.let { return it }
        if ('O' in name) getSystemWithoutTypos(name.replace('O', '0'), regionHint, systemHints)?.let { return it }
        return null
    }

    private fun getSystemWithoutTypos(
        name: String,
        regionHint: String?,
        systemHints: List<Int>,
    ): String? {
        if (name in systemNames) return name
        lowercaseSystemNames[name.lowercase()]?.let { return it }
        val candidates = when (name.length) {
            4 -> shortened4[name.lowercase()] ?: emptyList()
            3 -> shortened3[name.lowercase()] ?: emptyList()
            2 -> shortened2[name.lowercase()] ?: emptyList()
            else -> emptyList()
        }
        return if (candidates.size > 1) {
            candidates.singleOrNull { candidate ->
                regionNamesBySystemName[candidate] == regionHint
            } ?: candidates.singleOrNull { candidate ->
                val systemId = systemIdsByName[candidate]
                systemId in systemHints
            }
        } else {
            candidates.firstOrNull()
        }
    }

    fun getSystemSunTypeId(name: String): Int {
        return sunTypes[name] ?: DEFAULT_SUN_TYPE
    }

    fun getSystemId(name: String): Int? {
        return systemIdsByName[name]
    }

    fun getSystemName(id: Int): String? {
        return systemNamesById[id]
    }

    fun getSystem(name: String): MapSolarSystem? {
        val id = getSystemId(name) ?: return null
        return systemsById[id]
    }

    fun getSystem(id: Int): MapSolarSystem? {
        return systemsById[id]
    }

    fun getSystemSecurity(id: Int): Double? {
        return mapSolarSystems.firstOrNull { it.id == id }?.security
    }

    fun getKnownSpaceRegions(): List<String> {
        val invalidRegions = """^([A-Z]-R[0-9]{5}|ADR[0-9]{2}|VR-[0-9]{2}|No Name)$""".toRegex()
        return mapRegions
            .map { it.name }
            .filterNot {
                it.matches(invalidRegions)
            }
    }

    fun getRegionBySystem(systemName: String): String? {
        return regionNamesBySystemName[systemName]
    }

    fun getRegionIdBySystemId(systemId: Int): Int? {
        return regionIdBySystemId[systemId]
    }

    fun getSystems() = mapSolarSystems

    /**
     * Non-NPC Null-Sec systems
     */
    fun getSovSystems(): List<MapSolarSystem> {
        val npcNullRegions = listOf("Curse", "Great Wildlands", "Outer Ring", "Stain", "Syndicate", "Venal", "Yasna Zakh")
        val npcNullConstellations = listOf("38G6-L", "N-K4Q0", "Phoenix", "U-7RBK", "XPJ1-6", "6-UCYU")
        val npcNullRegionIds = mapRegions.filter { it.name in npcNullRegions }.map { it.id }
        val npcNullConstellationIds = mapConstellations.filter { it.name in npcNullConstellations }.map { it.id }
        return mapSolarSystems
            .filter { system ->
                system.regionId !in npcNullRegionIds &&
                    system.regionId < 11000000 && // K-space regions
                    system.constellationId !in npcNullConstellationIds
            }
            .filter { system ->
                system.security.roundSecurity() <= 0.0
            }
    }
}
