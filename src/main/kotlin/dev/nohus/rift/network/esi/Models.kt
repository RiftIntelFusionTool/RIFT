package dev.nohus.rift.network.esi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniverseIdsResponse(
    @SerialName("characters")
    val characters: List<UniverseIdsCharacter>? = null,
)

@Serializable
data class UniverseIdsCharacter(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
)

@Serializable
data class CharactersIdCharacter(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("corporation_id")
    val corporationId: Int,
    @SerialName("name")
    val name: String,
    @SerialName("title")
    val title: String? = null,
)

@Serializable
data class CorporationsIdCorporation(
    @SerialName("name")
    val name: String,
    @SerialName("ticker")
    val ticker: String,
)

@Serializable
data class AlliancesIdAlliance(
    @SerialName("name")
    val name: String,
    @SerialName("ticker")
    val ticker: String,
)

@Serializable
data class CharacterIdOnline(
    @SerialName("last_login")
    val lastLogin: String? = null,
    @SerialName("last_logout")
    val lastLogout: String? = null,
    @SerialName("logins")
    val logins: Int? = null,
    @SerialName("online")
    val isOnline: Boolean,
)

@Serializable
data class CharacterIdLocation(
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("station_id")
    val stationId: Int? = null,
    @SerialName("structure_id")
    val structureId: Long? = null,
)

@Serializable
data class UniverseStationsId(
    @SerialName("name")
    val name: String,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class UniverseStructuresId(
    @SerialName("name")
    val name: String,
    @SerialName("type_id")
    val typeId: Int,
)
