package dev.nohus.rift.database.static

import org.jetbrains.exposed.sql.Table

object SolarSystems : Table() {
    val solarSystemId = integer("solarSystemId")
    val solarSystemName = varchar("solarSystemName", 100)
    val regionId = integer("regionId")
    val constellationId = integer("constellationId")
    val sunTypeId = integer("sunTypeID")
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val security = double("security")
    override val primaryKey = PrimaryKey(solarSystemId)
}

object Regions : Table() {
    val regionId = integer("regionId")
    val regionName = varchar("regionName", 100)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    override val primaryKey = PrimaryKey(regionId)
}

object Constellations : Table() {
    val constellationId = integer("constellationId")
    val constellationName = varchar("constellationName", 100)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    override val primaryKey = PrimaryKey(constellationId)
}

object MapLayout : Table() {
    val regionId = integer("regionId")
    val solarSystemId = integer("solarSystemId")
    val x = integer("x")
    val y = integer("y")
}

object RegionMapLayout : Table() {
    val regionId = integer("regionId")
    val x = integer("x")
    val y = integer("y")
}

object Ships : Table() {
    val typeId = integer("typeId")
    val name = varchar("name", 100)
    val shipClass = varchar("class", 100)
    override val primaryKey = PrimaryKey(typeId)
}

object Types : Table() {
    val typeId = integer("typeId")
    val typeName = varchar("typeName", 100)
    val volume = float("volume")
    val iconId = integer("iconID").nullable()
    override val primaryKey = PrimaryKey(typeId)
}

object StarGates : Table() {
    val fromSystemId = integer("fromSystemId")
    val toSystemId = integer("toSystemId")
    val starGateTypeId = integer("starGateTypeId")
}

object Stations : Table() {
    val id = integer("id")
    val typeId = integer("typeId")
    val systemId = integer("systemId")
    val name = varchar("name", 100)
    override val primaryKey = PrimaryKey(id)
}
