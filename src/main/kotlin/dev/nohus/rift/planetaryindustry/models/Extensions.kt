package dev.nohus.rift.planetaryindustry.models

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.pi_commandcenter
import dev.nohus.rift.generated.resources.pi_ecu
import dev.nohus.rift.generated.resources.pi_processor
import dev.nohus.rift.generated.resources.pi_processoradvanced
import dev.nohus.rift.generated.resources.pi_processorhightech
import dev.nohus.rift.generated.resources.pi_spaceport
import dev.nohus.rift.generated.resources.pi_storage
import dev.nohus.rift.repositories.PlanetsRepository.Planet
import dev.nohus.rift.utils.plural
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.ceil
import kotlin.math.pow

fun Pin.isRouted(routes: List<Route>): RoutedState {
    val isInputRouted = if (this is Pin.Factory) {
        val inputTypes = schematic?.inputs?.map { it.key.id } ?: emptyList()
        val inputTypesReceived = routes.filter { it.destinationPinId == id }.map { it.type.id }.toSet()
        inputTypes.all { it in inputTypesReceived }
    } else {
        true
    }
    val isOutputRouted = if (this is Pin.Factory || this is Pin.Extractor) {
        routes.any { it.sourcePinId == id }
    } else {
        true
    }
    if (!isInputRouted) return RoutedState.InputNotRouted
    if (!isOutputRouted) return RoutedState.OutputNotRouted
    return RoutedState.Routed
}

fun Pin.getName(): String {
    val name = name.substringAfter(" ")
    val nameWithDesignator = "$name $designator"
    return when (this) {
        is Pin.Extractor -> {
            val heads = heads?.size ?: 0
            "$nameWithDesignator with $heads head${heads.plural}"
        }
        is Pin.CommandCenter -> "$nameWithDesignator Level $level"
        else -> nameWithDesignator
    }
}

fun Pin.getIcon(): DrawableResource {
    return when {
        "Extractor Control Unit" in name -> Res.drawable.pi_ecu
        "Production Plant" in name -> Res.drawable.pi_processorhightech
        "Advanced Industry Facility" in name -> Res.drawable.pi_processoradvanced
        "Industry Facility" in name -> Res.drawable.pi_processor
        "Command Center" in name -> Res.drawable.pi_commandcenter
        "Launchpad" in name -> Res.drawable.pi_spaceport
        "Storage" in name -> Res.drawable.pi_storage
        else -> Res.drawable.pi_commandcenter
    }
}

fun Pin.getCapacity(): Int? {
    return when (this) {
        is Pin.Extractor -> null
        is Pin.Factory -> null
        is Pin.Storage -> 12_000
        is Pin.CommandCenter -> 500
        is Pin.Launchpad -> 10_000
    }
}

fun Pin.getCpuPowerUsage(): Pair<Int, Int> {
    return when (this) {
        is Pin.CommandCenter -> 0 to 0
        is Pin.Extractor -> {
            val headCount = heads?.size ?: 0
            val cpu = 400 + (headCount * 110)
            val power = 2600 + (headCount * 550)
            cpu to power
        }
        is Pin.Factory -> {
            if ("Advanced" in name) {
                500 to 700
            } else if ("High-Tech" in name) {
                1100 to 400
            } else {
                200 to 800
            }
        }
        is Pin.Launchpad -> 3600 to 700
        is Pin.Storage -> 500 to 700
    }
}

fun Link.getCpuPowerUsage(planetRadius: Float, pins: List<Pin>): Pair<Int, Int> {
    val baseCpu = 15
    val basePower = 10
    val cpuPerKm = 0.2
    val powerPerKm = 0.15
    val cpuLevelModifier = 1.4
    val powerLevelModifier = 1.2
    val lengthInMeters = getDistance(planetRadius, pins)

    val cpu = baseCpu + ceil(cpuPerKm * lengthInMeters / 1000.0 * (level.toFloat() + 1.0).pow(cpuLevelModifier)).toInt()
    val power = basePower + ceil(powerPerKm * lengthInMeters / 1000.0 * (level.toFloat() + 1.0).pow(powerLevelModifier)).toInt()
    return cpu to power
}

fun Link.getDistance(planetRadius: Float, pins: List<Pin>): Float {
    val sourcePin = pins.firstOrNull { it.id == sourcePinId } ?: return 0f
    val destinationPin = pins.firstOrNull { it.id == destinationPinId } ?: return 0f
    val from = SurfacePoint(planetRadius, sourcePin.longitude, sourcePin.latitude)
    val to = SurfacePoint(planetRadius, destinationPin.longitude, destinationPin.latitude)
    return from.getDistanceTo(to)
}

fun getCpuPowerUsage(planet: Planet, pins: List<Pin>, links: List<Link>): Pair<Int, Int> {
    val pinsUsage = pins.map { it.getCpuPowerUsage() }
    val linksUsage = links.map { it.getCpuPowerUsage(planet.radius, pins) }
    val totalUsage = pinsUsage + linksUsage
    return totalUsage.reduce { acc, it -> (acc.first + it.first) to (acc.second + it.second) }
}

fun getCpuPowerSupply(upgradeLevel: Int): Pair<Int, Int> {
    return when (upgradeLevel) {
        0 -> 1675 to 6000
        1 -> 7057 to 9000
        2 -> 12136 to 12000
        3 -> 17215 to 15000
        4 -> 21315 to 17000
        else -> 25415 to 19000
    }
}
