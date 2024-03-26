package dev.nohus.rift.network.evewho

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllianceResponse(
    @SerialName("characters")
    val characters: List<CharacterReference>,
    @SerialName("info")
    val info: List<AllianceInfo>,
)

@Serializable
data class AllianceInfo(
    @SerialName("alliance_id")
    val allianceId: Int,
    @SerialName("memberCount")
    val memberCount: Int,
    @SerialName("name")
    val name: String,
)

@Serializable
data class CorporationResponse(
    @SerialName("characters")
    val characters: List<CharacterReference>,
    @SerialName("info")
    val info: List<CorporationInfo>,
)

@Serializable
data class CorporationInfo(
    @SerialName("corporation_id")
    val corporationId: Int,
    @SerialName("memberCount")
    val memberCount: Int,
    @SerialName("name")
    val name: String,
)

@Serializable
data class CharacterReference(
    @SerialName("character_id")
    val characterId: Int,
    @SerialName("name")
    val name: String,
)

@Serializable
data class CharacterResponse(
    @SerialName("info")
    val info: List<CharacterInfo>,
)

@Serializable
data class CharacterInfo(
    @SerialName("alliance_id")
    val allianceId: Int,
    @SerialName("character_id")
    val characterId: Int,
    @SerialName("corporation_id")
    val corporationId: Int,
    @SerialName("faction_id")
    val factionId: Int,
    @SerialName("name")
    val name: String,
    @SerialName("sec_status")
    val secStatus: Double,
)
