package dev.nohus.rift.planetaryindustry

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.PlanetaryPin
import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.Link
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.PinStatus
import dev.nohus.rift.planetaryindustry.models.Route
import dev.nohus.rift.planetaryindustry.models.Usage
import dev.nohus.rift.planetaryindustry.models.getColonyStatus
import dev.nohus.rift.planetaryindustry.models.getCpuPowerSupply
import dev.nohus.rift.planetaryindustry.models.getCpuPowerUsage
import dev.nohus.rift.planetaryindustry.models.getStatus
import dev.nohus.rift.planetaryindustry.simulation.ColonySimulation
import dev.nohus.rift.planetaryindustry.simulation.ColonySimulation.SimulationEndCondition.UntilNow
import dev.nohus.rift.planetaryindustry.simulation.ColonySimulation.SimulationEndCondition.UntilWorkEnds
import dev.nohus.rift.repositories.GetSystemDistanceFromCharacterUseCase
import dev.nohus.rift.repositories.PlanetsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.math.pow
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class PlanetaryIndustryRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val planetsRepository: PlanetsRepository,
    private val planetaryIndustrySchematicsRepository: PlanetaryIndustrySchematicsRepository,
    private val typesRepository: TypesRepository,
    private val getSystemDistanceFromCharacterUseCase: GetSystemDistanceFromCharacterUseCase,
    private val characterLocationRepository: CharacterLocationRepository,
) {

    data class ColonyItem(
        val colony: Colony,
        val ffwdColony: Colony,
        val characterName: String?,
        val distance: Int?,
    )

    private val _colonies = MutableStateFlow<AsyncResource<Map<String, ColonyItem>>>(AsyncResource.Loading)
    val colonies = _colonies.asStateFlow()

    private val reloadFlow = MutableSharedFlow<Unit>()
    private val simulatingFlow = MutableSharedFlow<Unit>()
    private val loadingMutex = Mutex()
    private val simulatingMutex = Mutex()
    private var isRealtime = false

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                if (isRealtime) updateColonies()
            }
        }
        launch {
            while (true) {
                delay(15.minutes)
                updateColonies()
            }
        }
        launch {
            localCharactersRepository.characters.debounce(500).collect {
                updateColonies()
            }
        }
        launch {
            reloadFlow.collect {
                updateColonies()
            }
        }
        launch {
            simulatingFlow.collect {
                simulateColonies()
            }
        }
        launch {
            while (true) {
                delay(if (isRealtime) 5.seconds else 30.seconds)
                simulateColonies()
            }
        }
        launch {
            localCharactersRepository.characters.collect { characters ->
                updateItems {
                    val character = characters.firstOrNull { it.characterId == colony.characterId }
                    copy(characterName = character?.info?.success?.name)
                }
            }
        }
        launch {
            characterLocationRepository.locations.collect {
                updateItems {
                    copy(distance = getDistance(colony))
                }
            }
        }
    }

    suspend fun reload(showLoading: Boolean) {
        if (showLoading) {
            _colonies.value = AsyncResource.Loading
        }
        if (!loadingMutex.isLocked) reloadFlow.emit(Unit)
    }

    suspend fun requestSimulation() {
        if (!simulatingMutex.isLocked) simulatingFlow.emit(Unit)
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private suspend fun simulateColonies() {
        simulatingMutex.withLock {
            val entries = _colonies.value.success ?: return
            val newColonies = coroutineScope {
                entries.values.map { item ->
                    async(Dispatchers.Default) {
                        val colony = ColonySimulation(item.colony).simulate(UntilNow)

                        val ffwdColony = if (!colony.status.isWorking) {
                            colony // Colony is not working, nothing to fast-forward
                        } else if (item.ffwdColony.status.isWorking || item.ffwdColony.currentSimTime.isBefore(colony.currentSimTime)) {
                            ColonySimulation(colony).simulate(UntilWorkEnds) // Fast-forwarded colony is not simulated yet
                        } else {
                            item.ffwdColony // Fast-forwarded colony already simulated
                        }

                        colony.id to item.copy(colony = colony, ffwdColony = ffwdColony)
                    }
                }.awaitAll().toMap()
            }
            _colonies.value = AsyncResource.Ready(newColonies)
        }
    }

    private suspend fun updateColonies() {
        loadingMutex.withLock {
            val result = loadColonies()
            if (result is AsyncResource.Ready || _colonies.value is AsyncResource.Loading) {
                when (result) {
                    is AsyncResource.Ready -> {
                        val existing = _colonies.value.success ?: emptyMap()
                        val updatedColonies = result.value.map { new ->
                            val old = existing[new.id]
                            if (old != null && old.colony.checkpointSimTime >= new.checkpointSimTime) {
                                new.id to old
                            } else {
                                new.id to toItem(new, new)
                            }
                        }.toMap()
                        _colonies.value = AsyncResource.Ready(updatedColonies)
                    }
                    is AsyncResource.Error -> {
                        _colonies.value = result
                    }
                    is AsyncResource.Loading -> {
                        _colonies.value = result
                    }
                }
                simulatingFlow.emit(Unit)
            }
        }
    }

    private fun toItem(colony: Colony, ffwdColony: Colony): ColonyItem {
        val character = localCharactersRepository.characters.value.firstOrNull { it.characterId == colony.characterId }
        return ColonyItem(
            colony = colony,
            ffwdColony = ffwdColony,
            characterName = character?.info?.success?.name,
            distance = getDistance(colony),
        )
    }

    private fun updateItems(update: ColonyItem.() -> ColonyItem) {
        val resource = _colonies.value.map { items ->
            items.mapValues { (_, item) -> item.update() }
        }
        _colonies.update { resource }
    }

    private fun getDistance(colony: Colony): Int {
        return getSystemDistanceFromCharacterUseCase(
            systemId = colony.system.id,
            maxDistance = 9,
            withJumpBridges = true,
            characterId = colony.characterId,
        )
    }

    private suspend fun loadColonies(): AsyncResource<List<Colony>> = coroutineScope {
        val characters = localCharactersRepository.characters.value
            .filter { it.isAuthenticated }.map { it.characterId }

        val colonies = characters.map { characterId ->
            async { esiApi.getCharactersIdPlanets(characterId) }
        }.awaitAll().flatMap { it.success ?: return@coroutineScope AsyncResource.Error(null) }

        val details = colonies.map { colony ->
            async { colony to esiApi.getCharactersIdPlanetsId(colony.ownerId, colony.planetId) }
        }.awaitAll()
            .map { (colony, result) -> colony to (result.success ?: return@coroutineScope AsyncResource.Error(null)) }

        val coloniesWithDetails = details.mapNotNull { (colony, details) ->
            val system = solarSystemsRepository.getSystem(colony.solarSystemId) ?: return@mapNotNull null
            val planet = planetsRepository.getPlanetById(colony.planetId) ?: return@mapNotNull null
            val links = details.links.map { link ->
                Link(
                    sourcePinId = link.sourcePinId,
                    destinationPinId = link.destinationPinId,
                    level = link.linkLevel,
                )
            }
            val routes = details.routes.map { route ->
                Route(
                    type = typesRepository.getTypeOrPlaceholder(route.typeId),
                    sourcePinId = route.sourcePinId,
                    destinationPinId = route.destinationPinId,
                    routeId = route.routeId,
                    quantity = route.quantity.toLong(),
                    waypoints = route.waypoints,
                )
            }
            val pins = details.pins.mapNotNull { pin ->
                getPin(pin, colony.lastUpdate, colony.upgradeLevel, routes)
            }
            val (cpuUsage, powerUsage) = getCpuPowerUsage(planet, pins, links)
            val (cpuSupply, powerSupply) = getCpuPowerSupply(colony.upgradeLevel)
            val usage = Usage(cpuUsage, cpuSupply, powerUsage, powerSupply)
            Colony(
                id = "${colony.ownerId}-${colony.planetId}",
                checkpointSimTime = colony.lastUpdate,
                currentSimTime = colony.lastUpdate,
                characterId = colony.ownerId,
                planet = planet,
                type = colony.planetType,
                system = system,
                upgradeLevel = colony.upgradeLevel,
                usage = usage,
                links = links,
                pins = pins,
                routes = routes,
                status = getColonyStatus(pins),
            )
        }
        AsyncResource.Ready(coloniesWithDetails)
    }

    private fun getPin(
        pin: PlanetaryPin,
        lastUpdate: Instant,
        upgradeLevel: Int,
        routes: List<Route>,
    ): Pin? {
        val pinType = typesRepository.getTypeOrPlaceholder(pin.typeId)
        val name = pinType.name
        val contents = pin.contents?.associate {
            val type = typesRepository.getTypeOrPlaceholder(it.typeId)
            type to it.amount
        }?.toMutableMap() ?: mutableMapOf()
        val capacityUsed = pin.contents?.fold(0f) { acc, planetaryItem ->
            val volume = typesRepository.getType(planetaryItem.typeId)?.volume ?: 0f
            acc + (planetaryItem.amount * volume)
        } ?: 0f
        val designator = getDesignator(pin.pinId)

        return if ("Extractor Control Unit" in name) {
            val cycleTime = pin.extractor?.cycleTime?.let { Duration.ofSeconds(it.toLong()) }
            val activityState = pin.expiryTime != null && lastUpdate < pin.expiryTime && pin.lastCycleStart != null
            val productType = pin.extractor?.productTypeId?.let { typesRepository.getTypeOrPlaceholder(it) }
            Pin.Extractor(
                id = pin.pinId,
                type = pinType,
                name = name,
                designator = designator,
                lastRunTime = pin.lastCycleStart,
                contents = contents,
                capacityUsed = capacityUsed,
                isActive = activityState,
                latitude = pin.latitude,
                longitude = pin.longitude,
                expiryTime = pin.expiryTime,
                installTime = pin.installTime,
                cycleTime = cycleTime,
                headRadius = pin.extractor?.headRadius,
                heads = pin.extractor?.heads,
                productType = productType,
                baseValue = pin.extractor?.quantityPerCycle,
                status = PinStatus.Static,
            )
        } else if ("Industry Facility" in name || "Production Plant" in name) {
            val schematic = pin.schematicId?.let { planetaryIndustrySchematicsRepository.getSchematic(it) }
            val cycleTime = schematic?.cycleTime ?: Duration.ZERO
            val lastCycleAgo = Duration.between(pin.lastCycleStart ?: Instant.EPOCH, lastUpdate)
            val activityState = lastCycleAgo < cycleTime && pin.schematicId != null
            Pin.Factory(
                id = pin.pinId,
                type = pinType,
                name = name,
                designator = designator,
                lastRunTime = pin.lastCycleStart,
                contents = contents,
                capacityUsed = capacityUsed,
                isActive = activityState,
                latitude = pin.latitude,
                longitude = pin.longitude,
                schematic = schematic,
                hasReceivedInputs = true,
                receivedInputsLastCycle = true,
                lastCycleStartTime = pin.lastCycleStart,
                status = PinStatus.Static,
            )
        } else if ("Command Center" in name) {
            Pin.CommandCenter(
                id = pin.pinId,
                type = pinType,
                name = name,
                designator = designator,
                lastRunTime = pin.lastCycleStart,
                contents = contents,
                capacityUsed = capacityUsed,
                isActive = false,
                latitude = pin.latitude,
                longitude = pin.longitude,
                level = upgradeLevel,
                status = PinStatus.Static,
            )
        } else if ("Launchpad" in name) {
            Pin.Launchpad(
                id = pin.pinId,
                type = pinType,
                name = name,
                designator = designator,
                lastRunTime = pin.lastCycleStart,
                contents = contents,
                capacityUsed = capacityUsed,
                isActive = false,
                latitude = pin.latitude,
                longitude = pin.longitude,
                status = PinStatus.Static,
            )
        } else if ("Storage" in name) {
            Pin.Storage(
                id = pin.pinId,
                type = pinType,
                name = name,
                designator = designator,
                lastRunTime = pin.lastCycleStart,
                contents = contents,
                capacityUsed = capacityUsed,
                isActive = false,
                latitude = pin.latitude,
                longitude = pin.longitude,
                status = PinStatus.Static,
            )
        } else {
            logger.error { "Unknown pin: $name" }
            return null
        }.run {
            copy(status = getStatus(lastUpdate, routes))
        }
    }

    private fun getDesignator(id: Long): String {
        val characters = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return buildString {
            repeat(5) {
                val index = id / characters.lastIndex.toDouble().pow(it.toDouble()) % characters.lastIndex
                append(characters[index.toInt()])
                if (it == 1) append("-")
            }
        }
    }
}
