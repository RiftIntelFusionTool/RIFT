package dev.nohus.rift.assets

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.CharactersIdAsset
import dev.nohus.rift.network.esi.CharactersIdAssetLocationType
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.UniverseStationsId
import dev.nohus.rift.network.esi.UniverseStructuresId
import dev.nohus.rift.repositories.TypesRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import retrofit2.HttpException

private val logger = KotlinLogging.logger {}

@Single
class AssetsRepository(
    private val localCharactersRepository: LocalCharactersRepository,
    private val typesRepository: TypesRepository,
    private val planetaryIndustryCommoditiesRepository: PlanetaryIndustryCommoditiesRepository,
    private val esiApi: EsiApi,
) {

    data class Assets(
        val list: List<AssetWithLocation> = emptyList(),
        val isLoading: Boolean = true,
    )

    private val reloadEventFlow = MutableSharedFlow<Unit>()
    private val _assets = MutableStateFlow(Assets())
    val assets = _assets.asStateFlow()

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters.debounce(500).collect { characters ->
                // Load assets after all authenticated characters finished loading
                if (characters.none { it.isAuthenticated && it.info is AsyncResource.Loading }) {
                    reloadEventFlow.emit(Unit)
                }
            }
        }
        launch {
            reloadEventFlow.collectLatest {
                val ids = localCharactersRepository.characters.value.filter { it.isAuthenticated }.map { it.characterId }
                load(ids)
            }
        }
    }

    enum class LocationType {
        Container, AssetSafety, System, AbyssalSystem, Station, Structure, Other
    }

    data class AssetWithCharacter(
        val asset: CharactersIdAsset,
        val name: String?,
        val typeName: String,
        val characterId: Int,
    )

    data class AssetWithLocation(
        val asset: CharactersIdAsset,
        val name: String?,
        val typeName: String,
        val characterId: Int,
        val location: AssetLocation,
    )

    sealed interface AssetLocation {
        data class Station(
            val locationId: Long,
            val name: String,
            val systemId: Int,
        ) : AssetLocation
        data class Structure(
            val locationId: Long,
            val name: String,
            val systemId: Int,
        ) : AssetLocation
        data class System(
            val locationId: Long,
            val systemId: Int,
        ) : AssetLocation
        data class AssetSafety(
            val locationId: Long,
        ) : AssetLocation
        data class CustomsOffice(
            val locationId: Long,
        ) : AssetLocation
        data class Unknown(
            val locationId: Long,
        ) : AssetLocation
    }

    data class ResolvedAssetLocations(
        val stationsById: Map<Long, UniverseStationsId>,
        val structuresById: Map<Long, UniverseStructuresId>,
        val unresolveableIds: List<Long>,
    )

    suspend fun reload() {
        reloadEventFlow.emit(Unit)
    }

    private suspend fun load(characters: List<Int>) {
        _assets.update { it.copy(isLoading = true) }
        val assets = when (val result = loadAllAssetsWithLocations(characters)) {
            is Result.Success -> result.data
            is Result.Failure -> {
                logger.error(result.cause) { "Could not load assets" }
                _assets.update { it.copy(isLoading = false) }
                return
            }
        }
        _assets.update { it.copy(list = assets, isLoading = false) }
    }

    private suspend fun loadAllAssetsWithLocations(characters: List<Int>): Result<List<AssetWithLocation>> = withContext(Dispatchers.IO) {
        if (characters.isEmpty()) return@withContext Result.Success(emptyList())
        val allAssets = when (val result = loadAllAssets(characters)) {
            is Result.Success -> result.data
            is Result.Failure -> return@withContext result
        }
        val itemIds = allAssets.map { it.asset.itemId }.distinct()
        val resolvedAssetLocations = when (val result = resolveAssetLocations(allAssets, itemIds)) {
            is Result.Success -> result.data
            is Result.Failure -> return@withContext result
        }
        val allAssetsWithLocation = allAssets.map { assetWithCharacter ->
            val location = getAssetLocation(assetWithCharacter, itemIds, allAssets, resolvedAssetLocations.stationsById, resolvedAssetLocations.structuresById)
            AssetWithLocation(
                asset = assetWithCharacter.asset,
                name = assetWithCharacter.name,
                typeName = assetWithCharacter.typeName,
                characterId = assetWithCharacter.characterId,
                location = location,
            )
        }
        Result.Success(allAssetsWithLocation)
    }

    private suspend fun resolveAssetLocations(
        allAssets: List<AssetWithCharacter>,
        itemIds: List<Long>,
    ): Result<ResolvedAssetLocations> = coroutineScope {
        val stationIds = mutableListOf<Long>()
        val structureIds = mutableListOf<Long>()
        allAssets
            .groupBy { getLocationType(it.asset.locationId, it.asset.locationType, itemIds) }
            .forEach { (locationType, items) ->
                val locationIds = items.map { it.asset.locationId }.distinct()
                when (locationType) {
                    LocationType.Station -> stationIds += locationIds
                    LocationType.Structure -> structureIds += locationIds
                    else -> {}
                }
            }
        val stationsByIdDeferred = stationIds.map { stationId ->
            async { stationId to esiApi.getUniverseStationsId(stationId.toInt()) }
        }
        val structuresByIdDeferred = structureIds.map { structureId ->
            async {
                val characterId = allAssets.first { it.asset.locationId == structureId }.characterId
                structureId to esiApi.getUniverseStructuresId(structureId, characterId)
            }
        }
        val stationsById = stationsByIdDeferred.awaitAll().associate { (id, result) ->
            when (result) {
                is Result.Success -> id to result.data
                is Result.Failure -> return@coroutineScope result
            }
        }
        val (structuresById, unresolveableIds) = structuresByIdDeferred.awaitAll().map { (id, result) ->
            when (result) {
                is Result.Success -> id to result.data
                is Result.Failure -> run {
                    if ((result.cause as? HttpException)?.code() == 403) {
                        structureIds -= id
                        id to null
                    } else {
                        return@coroutineScope result
                    }
                }
            }
        }
            .partition { it.first in structureIds }
            .let { (structures, unresolveables) ->
                structures.associate { it.first to it.second!! } to unresolveables.map { it.first }
            }
        Result.Success(
            ResolvedAssetLocations(
                stationsById = stationsById,
                structuresById = structuresById,
                unresolveableIds = unresolveableIds,
            ),
        )
    }

    private suspend fun loadAllAssets(characters: List<Int>): Result<List<AssetWithCharacter>> = coroutineScope {
        val assets = characters.map { characterId ->
            async {
                var page = 1
                val assets = mutableListOf<CharactersIdAsset>()
                while (true) {
                    when (val result = esiApi.getCharactersIdAssets(page, characterId)) {
                        is Result.Success -> {
                            assets += result.data.body() ?: emptyList()
                            val pages = result.data.headers()["x-pages"]?.toIntOrNull() ?: 1
                            if (page >= pages) break
                            page++
                        }
                        is Result.Failure -> return@async result
                    }
                }
                typesRepository.resolveNamesFromEsi(assets.map { it.typeId })
                val names = assets
                    .map { it.itemId }
                    .distinct()
                    .chunked(1000)
                    .flatMap { itemIds ->
                        when (val result = esiApi.getCharactersIdAssetsNames(characterId, itemIds)) {
                            is Result.Success -> result.data
                            is Result.Failure -> return@async result
                        }
                    }
                    .associate { it.itemId to it.name.takeIf { it != "None" } }
                Result.Success(Triple(characterId, assets, names))
            }
        }.awaitAll().flatMap { result ->
            when (result) {
                is Result.Success -> result.data.second.map {
                    AssetWithCharacter(
                        asset = it,
                        name = result.data.third[it.itemId],
                        typeName = typesRepository.getTypeName(it.typeId) ?: "${it.typeId}",
                        characterId = result.data.first,
                    )
                }
                is Result.Failure -> return@coroutineScope result
            }
        }
        Result.Success(assets)
    }

    private fun getAssetLocation(
        assetWithCharacter: AssetWithCharacter,
        itemIds: List<Long>,
        allAssets: List<AssetWithCharacter>,
        stationsById: Map<Long, UniverseStationsId>,
        structuresById: Map<Long, UniverseStructuresId>,
    ): AssetLocation {
        val locationId = assetWithCharacter.asset.locationId
        val locationType = getLocationType(locationId, assetWithCharacter.asset.locationType, itemIds)
        return when (locationType) {
            LocationType.Container -> {
                val container = allAssets.first { it.asset.itemId == locationId }
                getAssetLocation(container, itemIds, allAssets, stationsById, structuresById)
            }
            LocationType.AssetSafety -> AssetLocation.AssetSafety(locationId)
            LocationType.System -> AssetLocation.System(locationId, locationId.toInt())
            LocationType.AbyssalSystem -> AssetLocation.System(locationId, locationId.toInt())
            LocationType.Station -> {
                val station = stationsById[locationId]!!
                AssetLocation.Station(locationId, station.name, station.systemId)
            }
            LocationType.Structure -> {
                val structure = structuresById[locationId]
                if (structure != null) {
                    AssetLocation.Structure(locationId, structure.name, structure.solarSystemId)
                } else {
                    val assetsInLocation = allAssets.filter { it.asset.locationId == locationId }.map { it.asset.typeId }
                    if (planetaryIndustryCommoditiesRepository.isPlanetaryIndustryItems(assetsInLocation)) {
                        AssetLocation.CustomsOffice(locationId)
                    } else {
                        AssetLocation.Unknown(locationId)
                    }
                }
            }
            LocationType.Other -> AssetLocation.Unknown(locationId)
        }
    }

    private fun getLocationType(locationId: Long, locationType: CharactersIdAssetLocationType, itemIds: List<Long>): LocationType {
        return when {
            locationId == 2004L -> LocationType.AssetSafety
            locationType == CharactersIdAssetLocationType.SolarSystem -> when (locationId) {
                in 30000000L..32000000L -> return LocationType.System
                in 32000000L..33000000L -> return LocationType.AbyssalSystem
                else -> LocationType.System
            }
            locationType == CharactersIdAssetLocationType.Station -> LocationType.Station
            locationType == CharactersIdAssetLocationType.Item -> when {
                locationId in itemIds -> return LocationType.Container
                else -> LocationType.Structure
            }
            else -> LocationType.Other
        }
    }
}
