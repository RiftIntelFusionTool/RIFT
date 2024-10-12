package dev.nohus.rift.planetaryindustry.models

import dev.nohus.rift.network.esi.PlanetaryExtractorHead
import dev.nohus.rift.planetaryindustry.PlanetaryIndustrySchematicsRepository.Schematic
import dev.nohus.rift.repositories.TypesRepository.Type
import java.time.Duration
import java.time.Instant

sealed class Pin(
    open val id: Long,
    open val type: Type,
    open val name: String,
    open val designator: String,
    open var lastRunTime: Instant?,
    open val contents: MutableMap<Type, Long>,
    open var capacityUsed: Float,
    open var isActive: Boolean,
    open val latitude: Float,
    open val longitude: Float,
    open var status: PinStatus,
) {
    data class Extractor(
        override val id: Long,
        override val type: Type,
        override val name: String,
        override val designator: String,
        override var lastRunTime: Instant?,
        override val contents: MutableMap<Type, Long>,
        override var capacityUsed: Float,
        override var isActive: Boolean,
        override val latitude: Float,
        override val longitude: Float,
        override var status: PinStatus,
        val expiryTime: Instant?,
        val installTime: Instant?,
        val cycleTime: Duration?,
        val headRadius: Float?,
        val heads: List<PlanetaryExtractorHead>?,
        val productType: Type?,
        val baseValue: Int?,
    ) : Pin(id, type, name, designator, lastRunTime, contents, capacityUsed, isActive, latitude, longitude, status)

    data class Factory(
        override val id: Long,
        override val type: Type,
        override val name: String,
        override val designator: String,
        override var lastRunTime: Instant?,
        override val contents: MutableMap<Type, Long>,
        override var capacityUsed: Float,
        override var isActive: Boolean,
        override val latitude: Float,
        override val longitude: Float,
        override var status: PinStatus,
        val schematic: Schematic?,
        var hasReceivedInputs: Boolean,
        var receivedInputsLastCycle: Boolean,
        var lastCycleStartTime: Instant?,
    ) : Pin(id, type, name, designator, lastRunTime, contents, capacityUsed, isActive, latitude, longitude, status)

    data class Storage(
        override val id: Long,
        override val type: Type,
        override val name: String,
        override val designator: String,
        override var lastRunTime: Instant?,
        override val contents: MutableMap<Type, Long>,
        override var capacityUsed: Float,
        override var isActive: Boolean,
        override val latitude: Float,
        override val longitude: Float,
        override var status: PinStatus,
    ) : Pin(id, type, name, designator, lastRunTime, contents, capacityUsed, isActive, latitude, longitude, status)

    data class Launchpad(
        override val id: Long,
        override val type: Type,
        override val name: String,
        override val designator: String,
        override var lastRunTime: Instant?,
        override val contents: MutableMap<Type, Long>,
        override var capacityUsed: Float,
        override var isActive: Boolean,
        override val latitude: Float,
        override val longitude: Float,
        override var status: PinStatus,
    ) : Pin(id, type, name, designator, lastRunTime, contents, capacityUsed, isActive, latitude, longitude, status)

    data class CommandCenter(
        override val id: Long,
        override val type: Type,
        override val name: String,
        override val designator: String,
        override var lastRunTime: Instant?,
        override val contents: MutableMap<Type, Long>,
        override var capacityUsed: Float,
        override var isActive: Boolean,
        override val latitude: Float,
        override val longitude: Float,
        override var status: PinStatus,
        val level: Int,
    ) : Pin(id, type, name, designator, lastRunTime, contents, capacityUsed, isActive, latitude, longitude, status)
}
