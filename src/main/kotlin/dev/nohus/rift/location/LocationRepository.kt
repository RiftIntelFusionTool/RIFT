package dev.nohus.rift.location

import dev.nohus.rift.network.esi.EsiApi
import org.koin.core.annotation.Single

@Single
class LocationRepository(
    private val esiApi: EsiApi,
) {

    data class Station(
        val stationId: Int,
        val name: String,
        val typeId: Int,
        val solarSystemId: Int,
    )

    data class Structure(
        val structureId: Long,
        val name: String,
        val typeId: Int?,
        val solarSystemId: Int,
    )

    suspend fun getStation(stationId: Int?): Station? {
        if (stationId == null) return null
        return esiApi.getUniverseStationsId(stationId).success
            ?.let { Station(stationId, it.name, it.typeId, it.systemId) }
    }

    suspend fun getStructure(structureId: Long?, characterId: Int): Structure? {
        if (structureId == null) return null
        return esiApi.getUniverseStructuresId(structureId, characterId).success
            ?.let { Structure(structureId, it.name, it.typeId, it.solarSystemId) }
    }
}
