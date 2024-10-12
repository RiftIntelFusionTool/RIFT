package dev.nohus.rift.planetaryindustry.simulation

import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.ColonyStatus
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.PinStatus.StorageFull
import dev.nohus.rift.planetaryindustry.models.Route
import dev.nohus.rift.planetaryindustry.models.getCapacity
import dev.nohus.rift.planetaryindustry.models.getColonyStatus
import dev.nohus.rift.planetaryindustry.models.getStatus
import dev.nohus.rift.planetaryindustry.simulation.ColonySimulation.SimulationEndCondition.UntilNow
import dev.nohus.rift.planetaryindustry.simulation.ColonySimulation.SimulationEndCondition.UntilWorkEnds
import dev.nohus.rift.planetaryindustry.simulation.ExtractionSimulation.Companion.getProgramOutput
import dev.nohus.rift.repositories.TypesRepository.Type
import java.time.Duration
import java.time.Instant
import java.util.PriorityQueue
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

class ColonySimulation(colony: Colony) {

    private val colony: Colony = colony.clone()
    private val eventQueue = PriorityQueue<Pair<Instant, Long>>(compareBy { it.first })
    private var currentSimTime: Instant = colony.currentSimTime
    private var simEndTime: Instant? = null
    private val currentRealTime: Instant = Instant.now()

    sealed interface SimulationEndCondition {
        data object UntilNow : SimulationEndCondition
        data object UntilWorkEnds : SimulationEndCondition
    }

    private val SimulationEndCondition.simEndTime: Instant
        get() = when (this) {
            UntilNow -> currentRealTime
            UntilWorkEnds -> Instant.MAX
        }

    fun simulate(until: SimulationEndCondition): Colony {
        val currentSimTime = runSimulation(until)
        return colony.copy(
            currentSimTime = currentSimTime,
            status = getColonyStatus(currentSimTime),
        )
    }

    private fun getColonyStatus(currentSimTime: Instant): ColonyStatus {
        colony.pins.forEach { it.status = it.getStatus(currentSimTime, colony.routes) }
        return getColonyStatus(colony.pins)
    }

    private fun runSimulation(until: SimulationEndCondition): Instant {
        if (until == UntilWorkEnds && !getColonyStatus(currentSimTime).isWorking) {
            return currentSimTime // This colony is already not working
        }

        initializeSimulation(until)
        while (eventQueue.isNotEmpty()) {
            val (simTime, simPinId) = eventQueue.remove()

            if (until == UntilNow && simTime > currentRealTime) return currentRealTime
            simEndTime?.let { if (simTime.isAfter(it)) return currentSimTime }

            currentSimTime = simTime
            val simPin = getPin(simPinId)
            if (!simPin.canRun(until.simEndTime)) continue
            evaluatePin(simPin)

            if (until == UntilWorkEnds && simEndTime == null) {
                val status = getColonyStatus(currentSimTime)
                if (!status.isWorking) {
                    if (status.pins.any { it.status == StorageFull }) {
                        return currentSimTime // Don't simulate other pins
                    } else {
                        simEndTime = simTime // Continue simulating other pins until past this instant
                    }
                }
            }
        }
        return when (until) {
            UntilNow -> currentRealTime
            UntilWorkEnds -> currentSimTime
        }
    }

    private fun initializeSimulation(until: SimulationEndCondition) {
        eventQueue.clear()
        for (pin in colony.pins) {
            if (pin.canRun(until.simEndTime)) {
                schedulePin(pin)
            }
        }
    }

    private fun schedulePin(pin: Pin) {
        val nextRunTime = pin.getNextRunTime()
        val element = eventQueue.firstOrNull { it.second == pin.id }
        if (element != null) {
            val (evalTime, evalPinId) = element
            if (nextRunTime == null || nextRunTime < evalTime) {
                eventQueue -= (evalTime to evalPinId)
            } else {
                return
            }
        }

        if (nextRunTime == null || nextRunTime < currentSimTime) {
            addTimer(pin.id, currentSimTime)
        } else {
            addTimer(pin.id, nextRunTime)
        }
    }

    private fun addTimer(pinId: Long, runTime: Instant) {
        eventQueue.add(runTime to pinId)
    }

    private fun evaluatePin(pin: Pin) {
        if (!pin.canActivate() && !pin.isActive()) {
            return
        }
        val commodities = getCommoditiesProducedByPin(pin)
        if (pin.isConsumer()) {
            routeCommodityInput(pin)
        }
        if (pin.isActive() || pin.canActivate()) {
            schedulePin(pin)
        }
        if (commodities.isEmpty()) {
            return
        }
        routeCommodityOutput(pin, commodities.toMutableMap())
    }

    private fun getCommoditiesProducedByPin(pin: Pin): Map<Type, Long> {
        return pin.run(currentSimTime)
    }

    private fun routeCommodityInput(destinationPin: Pin) {
        val routesToEvaluate = getDestinationRoutesForPin(destinationPin.id)
        for (route in routesToEvaluate) {
            val sourcePinId = route.sourcePinId
            val sourcePin = getPinOrNull(sourcePinId) ?: continue
            if (!sourcePin.isStorage()) continue
            val storedCommodities = sourcePin.contents
            if (storedCommodities.isEmpty()) continue
            executeRoute(route, storedCommodities)
        }
    }

    private fun executeRoute(route: Route, commodities: Map<Type, Long>) {
        val sourceId = route.sourcePinId
        val destinationId = route.destinationPinId
        val type = route.type
        val quantity = route.quantity
        transferCommodities(sourceId, destinationId, type, quantity, commodities)
    }

    private fun transferCommodities(
        sourceId: Long,
        destinationId: Long,
        type: Type,
        quantity: Long,
        commodities: Map<Type, Long>,
        maxAmount: Long? = null
    ): Pair<Type?, Long> {
        val sourcePin = getPin(sourceId)
        if (type !in commodities) {
            return null to 0
        }
        var amountToMove = min(commodities.getValue(type), quantity)
        if (maxAmount != null) {
            amountToMove = min(maxAmount, amountToMove)
        }
        if (amountToMove <= 0) {
            return null to 0
        }
        val destinationPin = getPin(destinationId)
        val amountMoved = destinationPin.addCommodity(type, amountToMove)
        if (sourcePin.isStorage()) {
            sourcePin.removeCommodity(type, amountMoved)
        }
        return type to amountMoved
    }

    private fun routeCommodityOutput(sourcePin: Pin, commodities: MutableMap<Type, Long>) {
        val pinsReceivingCommodities = mutableMapOf<Long, MutableMap<Type, Long>>()
        var done = false
        val (processorRoutes, storageRoutes) = getSortedRoutesForPin(sourcePin.id, commodities)
        for ((isStorageRoutes, listOfRoutes) in listOf(false to processorRoutes, true to storageRoutes)) {
            if (done) break
            while (listOfRoutes.isNotEmpty()) {
                val (_, destinationId, commodityType, qty) = listOfRoutes.remove()
                var maxAmount: Long? = null
                if (isStorageRoutes) {
                    maxAmount = ceil(commodities.getOrDefault(commodityType, 0).toFloat() / (listOfRoutes.size + 1)).roundToLong()
                }
                val (type, transferredQuantity) = transferCommodities(sourcePin.id, destinationId, commodityType, qty, commodities, maxAmount)
                if (type != null) {
                    if (type in commodities) {
                        commodities[type] = commodities.getValue(type) - transferredQuantity
                        if (commodities.getValue(type) <= 0) {
                            commodities -= type
                        }
                    }
                    if (transferredQuantity > 0) {
                        if (destinationId !in pinsReceivingCommodities) {
                            pinsReceivingCommodities[destinationId] = mutableMapOf()
                        }
                        if (type !in pinsReceivingCommodities.getValue(destinationId)) {
                            pinsReceivingCommodities.getValue(destinationId) += type to 0
                        }
                        pinsReceivingCommodities.getValue(destinationId).let { map ->
                            map[type] = map.getValue(type) + transferredQuantity
                        }
                    }
                }
                if (commodities.isEmpty()) {
                    done = true
                    break
                }
            }
        }

        for ((receivingPinID, commoditiesAdded) in pinsReceivingCommodities) {
            val receivingPin = getPin(receivingPinID)
            if (receivingPin.isConsumer()) {
                schedulePin(receivingPin)
            }
            if (!sourcePin.isStorage() && receivingPin.isStorage()) {
                routeCommodityOutput(receivingPin, commoditiesAdded)
            }
        }
    }

    private fun getDestinationRoutesForPin(pinId: Long): List<Route> {
        return colony.routes.filter { it.destinationPinId == pinId }
    }

    data class SortedRoute(
        val sortingKey: Float,
        val destinationId: Long,
        val commodityType: Type,
        val quantity: Long,
    )
    private fun getSortedRoutesForPin(pinId: Long, commodities: Map<Type, Long>): Pair<PriorityQueue<SortedRoute>, PriorityQueue<SortedRoute>> {
        val processorRoutes = PriorityQueue<SortedRoute>(compareBy { it.sortingKey })
        val storageRoutes = PriorityQueue<SortedRoute>(compareBy { it.sortingKey })
        for (route in colony.routes.filter { it.sourcePinId == pinId }) {
            if (route.type !in commodities.keys) continue
            val destinationPin = getPin(route.destinationPinId)
            if (destinationPin is Pin.Factory) {
                processorRoutes.add(SortedRoute(destinationPin.getInputBufferState(), route.destinationPinId, route.type, route.quantity))
            } else {
                processorRoutes.add(SortedRoute(destinationPin.getFreeSpace(), route.destinationPinId, route.type, route.quantity))
            }
        }
        return processorRoutes to storageRoutes
    }

    private fun getPin(pinId: Long): Pin {
        return colony.pins.first { it.id == pinId }
    }

    private fun getPinOrNull(pinId: Long): Pin? {
        return colony.pins.firstOrNull { it.id == pinId }
    }

    private fun Pin.canActivate(): Boolean {
        when (this) {
            is Pin.Extractor -> {
                if (!isActive) return false
                return productType != null
            }
            is Pin.Factory -> {
                if (schematic == null) return false
                if (isActive()) return true
                if (hasReceivedInputs || receivedInputsLastCycle) return true
                if (hasEnoughInputs()) return false
            }
            is Pin.Storage, is Pin.CommandCenter, is Pin.Launchpad -> return false
        }
        return true
    }

    private fun Pin.isActive(): Boolean {
        return when (this) {
            is Pin.Extractor -> productType != null && isActive
            is Pin.Factory -> isActive
            is Pin.Storage, is Pin.CommandCenter, is Pin.Launchpad -> isActive
        }
    }

    private fun Pin.canRun(runTime: Instant): Boolean {
        when (this) {
            is Pin.Extractor -> {
                if (!canActivate()) return false
                val nextRunTime = getNextRunTime()
                return nextRunTime == null || nextRunTime <= runTime
            }
            is Pin.Factory -> {
                if (!isActive() && !canActivate()) return false
                val nextRunTime = getNextRunTime()
                return nextRunTime == null || nextRunTime <= runTime
            }
            is Pin.Storage, is Pin.CommandCenter, is Pin.Launchpad -> return false
        }
    }

    private fun Pin.getNextRunTime(): Instant? {
        if (this is Pin.Factory) {
            if (!isActive() && hasEnoughInputs()) return null
        }
        return lastRunTime?.let { it + getCycleTime() }
    }

    private fun Pin.getCycleTime(): Duration {
        return when (this) {
            is Pin.Extractor -> cycleTime ?: Duration.ZERO
            is Pin.Factory -> schematic?.cycleTime ?: Duration.ZERO
            is Pin.Storage, is Pin.CommandCenter, is Pin.Launchpad -> Duration.ZERO
        }
    }

    private fun Pin.run(runTime: Instant): Map<Type, Long> {
        when (this) {
            is Pin.Extractor -> {
                lastRunTime = runTime
                if (productType == null) return emptyMap()
                val products = mutableMapOf<Type, Long>()
                if (isActive()) {
                    if (baseValue != null && installTime != null && cycleTime != null) {
                        products[productType] = getProgramOutput(baseValue, installTime, runTime, cycleTime)
                    }
                    if (expiryTime != null && expiryTime <= runTime) {
                        isActive = false
                    }
                }
                return products
            }
            is Pin.Factory -> {
                var products = mutableMapOf<Type, Long>()
                if (isActive()) {
                    products = schematic?.let { mutableMapOf(it.outputType to it.outputQuantity) } ?: mutableMapOf()
                }
                var canConsume = true
                for ((demandType, demandQuantity) in demands) {
                    if (demandType !in contents) {
                        canConsume = false
                        break
                    }
                    if (demandQuantity > contents.getValue(demandType)) {
                        canConsume = false
                        break
                    }
                }

                if (canConsume) {
                    for ((demandType, demandQuantity) in demands) {
                        removeCommodity(demandType, demandQuantity)
                    }
                    isActive = true
                    lastCycleStartTime = runTime
                } else {
                    isActive = false
                }
                receivedInputsLastCycle = hasReceivedInputs
                hasReceivedInputs = false
                lastRunTime = runTime
                return products
            }
            is Pin.Storage, is Pin.CommandCenter, is Pin.Launchpad -> {
                lastRunTime = runTime
                if (isActive) isActive = false
                return emptyMap()
            }
        }
    }

    private fun Pin.isConsumer(): Boolean {
        if (this is Pin.Factory) return true
        return false
    }

    private fun Pin.Factory.hasEnoughInputs(): Boolean {
        for ((demandTypeId, demandQuantity) in demands) {
            if (demandTypeId !in contents) return false
            if (demandQuantity > contents.getValue(demandTypeId)) return false
        }
        return true
    }

    private fun Pin.Factory.getInputBufferState(): Float {
        var productsRatio = 0f
        for ((typeId, quantity) in demands) {
            productsRatio += (contents[typeId] ?: 0L).toFloat() / quantity
        }
        return (1f - productsRatio) / demands.size
    }

    private val Pin.Factory.demands: Map<Type, Long>
        get() = schematic?.inputs ?: emptyMap()

    private fun Pin.isStorage(): Boolean {
        return when (this) {
            is Pin.Extractor -> false
            is Pin.Factory -> false
            is Pin.Storage -> true
            is Pin.CommandCenter -> true
            is Pin.Launchpad -> true
        }
    }

    private fun Pin.getFreeSpace(): Float {
        val capacity = getCapacity()!!
        var usedSpace = 0f
        for ((type, qty) in contents) {
            val volume = type.volume
            usedSpace += volume * qty
        }
        return capacity - usedSpace
    }

    private fun Pin.addCommodity(type: Type, quantity: Long): Long {
        return when (this) {
            is Pin.Extractor -> return 0
            is Pin.Factory -> {
                val qtyAdded = addCommodityInternal(type, quantity)
                if (qtyAdded > 0) {
                    hasReceivedInputs = true
                }
                return qtyAdded
            }
            is Pin.Storage, is Pin.CommandCenter, is Pin.Launchpad -> addCommodityInternal(type, quantity)
        }
    }

    private fun Pin.addCommodityInternal(type: Type, quantity: Long): Long {
        val quantityToAdd = canAccept(type, quantity)
        if (quantityToAdd < 1) return 0
        if (getCapacity() != null) {
            capacityUsed += quantityToAdd * type.volume
        }
        if (type !in contents) {
            contents[type] = quantityToAdd
        } else {
            contents[type] = contents.getValue(type) + quantityToAdd
        }
        return quantityToAdd
    }

    private fun Pin.removeCommodity(type: Type, quantity: Long): Long {
        if (type !in contents) return 0
        val quantityRemoved: Long
        if (contents.getValue(type) <= quantity) {
            quantityRemoved = contents.getValue(type)
            contents -= type
        } else {
            quantityRemoved = quantity
            contents[type] = contents.getValue(type) - quantityRemoved
        }
        if (getCapacity() != null) {
            capacityUsed = max(0f, capacityUsed - type.volume * quantityRemoved)
        }
        return quantityRemoved
    }

    private fun Pin.canAccept(type: Type, quantity: Long): Long {
        when (this) {
            is Pin.Extractor -> return 0
            is Pin.Factory -> {
                if (type !in demands) return 0
                val quantity = if (quantity < 0) demands.getValue(type) else quantity
                var remainingSpace = demands.getValue(type)
                if (type in contents) {
                    remainingSpace = demands.getValue(type) - contents.getValue(type)
                }
                if (remainingSpace < quantity) {
                    return remainingSpace
                }
                return quantity
            }
            is Pin.Storage, is Pin.CommandCenter, is Pin.Launchpad -> {
                val volume = type.volume
                val newVolume = volume * quantity
                val capacityRemaining = getCapacityRemaining()
                if (newVolume > capacityRemaining || quantity == -1L) {
                    return (capacityRemaining / volume).toLong()
                } else {
                    return quantity
                }
            }
        }
    }

    private fun Pin.getCapacityRemaining(): Float {
        val capacityRemaining = max(0f, (getCapacity()?.toFloat() ?: 0f) - capacityUsed)
        return capacityRemaining
    }
}
