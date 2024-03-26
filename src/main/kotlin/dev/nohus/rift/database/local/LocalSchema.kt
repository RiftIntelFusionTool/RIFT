package dev.nohus.rift.database.local

import org.jetbrains.exposed.sql.Table

object Characters : Table() {
    val name = varchar("name", 37)
    val characterId = integer("characterId").nullable()
    val isInactive = bool("isInactive").nullable()
    val exists = bool("exists")
    val checkTimestamp = long("checkTimestamp")
    override val primaryKey = PrimaryKey(name)
}

object Placeholder
