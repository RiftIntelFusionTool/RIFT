package dev.nohus.rift.planetaryindustry.models

import dev.nohus.rift.planetaryindustry.models.PinStatus.Extracting
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorExpired
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorInactive
import dev.nohus.rift.planetaryindustry.models.PinStatus.FactoryIdle
import dev.nohus.rift.planetaryindustry.models.PinStatus.InputNotRouted
import dev.nohus.rift.planetaryindustry.models.PinStatus.NotSetup
import dev.nohus.rift.planetaryindustry.models.PinStatus.OutputNotRouted
import dev.nohus.rift.planetaryindustry.models.PinStatus.Producing
import dev.nohus.rift.planetaryindustry.models.PinStatus.Static
import dev.nohus.rift.planetaryindustry.models.PinStatus.StorageFull
import java.time.Instant
import kotlin.math.max

/**
 * @param order - sorting order
 * @param pins - relevant pins causing this status state
 */
sealed class ColonyStatus(
    val order: Int,
    val isWorking: Boolean,
    open val pins: List<Pin>,
) {
    data class NotSetup(override val pins: List<Pin>) : ColonyStatus(1, false, pins)
    data class NeedsAttention(override val pins: List<Pin>) : ColonyStatus(2, false, pins)
    data class Idle(override val pins: List<Pin>) : ColonyStatus(3, false, pins)
    data class Producing(override val pins: List<Pin>) : ColonyStatus(4, true, pins)
    data class Extracting(override val pins: List<Pin>) : ColonyStatus(5, true, pins)
}

sealed interface PinStatus {
    data object Static : PinStatus
    data object Extracting : PinStatus
    data object Producing : PinStatus
    data object NotSetup : PinStatus
    data object InputNotRouted : PinStatus
    data object OutputNotRouted : PinStatus
    data object ExtractorExpired : PinStatus
    data object ExtractorInactive : PinStatus
    data object StorageFull : PinStatus
    data object FactoryIdle : PinStatus
}

fun getColonyStatus(pins: List<Pin>): ColonyStatus {
    val notSetupPins = pins.filter { it.status in listOf(NotSetup, InputNotRouted, OutputNotRouted) }
    if (notSetupPins.isNotEmpty()) return ColonyStatus.NotSetup(notSetupPins)
    val needsAttentionPins = pins.filter { it.status in listOf(ExtractorExpired, ExtractorInactive, StorageFull) }
    if (needsAttentionPins.isNotEmpty()) return ColonyStatus.NeedsAttention(needsAttentionPins)
    val extractingPins = pins.filter { it.status == Extracting }
    if (extractingPins.isNotEmpty()) return ColonyStatus.Extracting(extractingPins)
    val producingPins = pins.filter { it.status == Producing }
    if (producingPins.isNotEmpty()) return ColonyStatus.Producing(producingPins)
    return ColonyStatus.Idle(emptyList())
}

fun Pin.getStatus(now: Instant, routes: List<Route>): PinStatus {
    when (this) {
        is Pin.Extractor -> {
            val isSetup = installTime != null && expiryTime != null && cycleTime != null && baseValue != null && productType != null
            if (!isSetup) return NotSetup
            val hasExpired = expiryTime != null && expiryTime <= now
            if (hasExpired) return ExtractorExpired
            when (isRouted(routes)) {
                RoutedState.Routed -> {}
                RoutedState.InputNotRouted -> return InputNotRouted
                RoutedState.OutputNotRouted -> return OutputNotRouted
            }
            if (isActive) return Extracting
            return ExtractorInactive
        }
        is Pin.Factory -> {
            if (schematic == null) return NotSetup
            when (isRouted(routes)) {
                RoutedState.Routed -> {}
                RoutedState.InputNotRouted -> return InputNotRouted
                RoutedState.OutputNotRouted -> return OutputNotRouted
            }
            if (isActive) return Producing
            return FactoryIdle
        }
        is Pin.CommandCenter, is Pin.Launchpad, is Pin.Storage -> {
            if (max(getCapacity()!!.toFloat() - capacityUsed, 0f) == 0f) {
                val hasIncomingRoutes = routes.any { it.destinationPinId == id }
                if (hasIncomingRoutes) return StorageFull
            }
            return Static
        }
    }
}
