package dev.nohus.rift.network.killboard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class EveKillKillmail(
    @SerialName("attackers")
    val attackers: List<EveKillAttacker>,
    @SerialName("dna")
    val dna: String? = null,
    @SerialName("fitting_value")
    val fittingValue: Double? = null,
    @SerialName("hash")
    val hash: String? = null,
    @SerialName("is_npc")
    val isNpc: Boolean? = null,
    @SerialName("is_solo")
    val isSolo: Boolean? = null,
    @SerialName("items")
    val items: List<EveKillItem>,
    @SerialName("kill_time")
    val killTime: String? = null,
    @SerialName("kill_time_str")
    @Serializable(with = IsoDateTimeSerializer::class)
    val killTimeStr: Instant? = null,
    @SerialName("killmail_id")
    val killmailId: Int? = null,
    @SerialName("last_modified")
    val lastModified: String? = null,
    @SerialName("near")
    val near: String? = null,
    @SerialName("point_value")
    val pointValue: Int? = null,
    @SerialName("region_id")
    val regionId: Int? = null,
    @SerialName("region_name")
    val regionName: String? = null,
    @SerialName("ship_value")
    val shipValue: Double? = null,
    @SerialName("system_id")
    val systemId: Int? = null,
    @SerialName("system_name")
    val systemName: String? = null,
    @SerialName("system_security")
    val systemSecurity: Double? = null,
    @SerialName("total_value")
    val totalValue: Double? = null,
    @SerialName("victim")
    val victim: EveKillVictim? = null,
    @SerialName("war_id")
    val warId: Int? = null,
    @SerialName("x")
    val x: Double? = null,
    @SerialName("y")
    val y: Double? = null,
    @SerialName("z")
    val z: Double? = null,
)

@Serializable
data class EveKillAttacker(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("alliance_image_url")
    val allianceImageUrl: String? = null,
    @SerialName("alliance_name")
    val allianceName: String? = null,
    @SerialName("character_id")
    val characterId: Int? = null,
    @SerialName("character_image_url")
    val characterImageUrl: String? = null,
    @SerialName("character_name")
    val characterName: String? = null,
    @SerialName("corporation_id")
    val corporationId: Int? = null,
    @SerialName("corporation_image_url")
    val corporationImageUrl: String? = null,
    @SerialName("corporation_name")
    val corporationName: String? = null,
    @SerialName("damage_done")
    val damageDone: Int? = null,
    @SerialName("faction_id")
    val factionId: Int? = null,
    @SerialName("faction_image_url")
    val factionImageUrl: String? = null,
    @SerialName("faction_name")
    val factionName: String? = null,
    @SerialName("final_blow")
    val finalBlow: Boolean? = null,
    @SerialName("points")
    val points: Int? = null,
    @SerialName("security_status")
    val securityStatus: Double? = null,
    @SerialName("ship_group_id")
    val shipGroupId: Int? = null,
    @SerialName("ship_group_name")
    val shipGroupName: String? = null,
    @SerialName("ship_id")
    val shipId: Int? = null,
    @SerialName("ship_image_url")
    val shipImageUrl: String? = null,
    @SerialName("ship_name")
    val shipName: String? = null,
    @SerialName("weapon_type_id")
    val weaponTypeId: Int? = null,
    @SerialName("weapon_type_name")
    val weaponTypeName: String? = null,
)

@Serializable
data class EveKillVictim(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("alliance_image_url")
    val allianceImageUrl: String? = null,
    @SerialName("alliance_name")
    val allianceName: String? = null,
    @SerialName("character_id")
    val characterId: Int? = null,
    @SerialName("character_image_url")
    val characterImageUrl: String? = null,
    @SerialName("character_name")
    val characterName: String? = null,
    @SerialName("corporation_id")
    val corporationId: Int? = null,
    @SerialName("corporation_image_url")
    val corporationImageUrl: String? = null,
    @SerialName("corporation_name")
    val corporationName: String? = null,
    @SerialName("damage_taken")
    val damageTaken: Int? = null,
    @SerialName("faction_id")
    val factionId: Int? = null,
    @SerialName("faction_image_url")
    val factionImageUrl: String? = null,
    @SerialName("faction_name")
    val factionName: String? = null,
    @SerialName("ship_group_id")
    val shipGroupId: Int? = null,
    @SerialName("ship_group_name")
    val shipGroupName: String? = null,
    @SerialName("ship_id")
    val shipId: Int? = null,
    @SerialName("ship_image_url")
    val shipImageUrl: String? = null,
    @SerialName("ship_name")
    val shipName: String? = null,
)

@Serializable
data class EveKillItem(
    @SerialName("category_id")
    val categoryId: Int? = null,
    @SerialName("flag")
    val flag: Int? = null,
    @SerialName("group_id")
    val groupId: Int? = null,
    @SerialName("group_name")
    val groupName: String? = null,
    @SerialName("qty_destroyed")
    val qtyDestroyed: Int? = null,
    @SerialName("qty_dropped")
    val qtyDropped: Int? = null,
    @SerialName("singleton")
    val singleton: Int? = null,
    @SerialName("type_id")
    val typeId: Int? = null,
    @SerialName("type_image_url")
    val typeImageUrl: String? = null,
    @SerialName("type_name")
    val typeName: String? = null,
    @SerialName("value")
    val value: Double? = null,
)
