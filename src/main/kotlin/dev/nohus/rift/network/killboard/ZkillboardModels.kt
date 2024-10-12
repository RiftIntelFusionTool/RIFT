package dev.nohus.rift.network.killboard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ZkillboardKillmail(
    @SerialName("attackers")
    val attackers: List<ZkillboardAttacker>,
    @SerialName("killmail_id")
    val killmailId: Int,
    @SerialName("killmail_time")
    @Serializable(with = IsoDateTimeSerializer::class)
    val killmailTime: Instant,
    @SerialName("moon_id")
    val moonId: Int? = null,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("victim")
    val victim: ZkillboardVictim,
    @SerialName("war_id")
    val warId: Int? = null,
    @SerialName("zkb")
    val zkb: Zkb,
)

@Serializable
data class ZkillboardAttacker(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("character_id")
    val characterId: Int? = null,
    @SerialName("corporation_id")
    val corporationId: Int? = null,
    @SerialName("damage_done")
    val damageDone: Int,
    @SerialName("faction_id")
    val factionId: Int? = null,
    @SerialName("final_blow")
    val finalBlow: Boolean,
    @SerialName("security_status")
    val securityStatus: Float,
    @SerialName("ship_type_id")
    val shipTypeId: Int? = null,
    @SerialName("weapon_type_id")
    val weaponTypeId: Int? = null,
)

@Serializable
data class ZkillboardVictim(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("character_id")
    val characterId: Int? = null,
    @SerialName("corporation_id")
    val corporationId: Int? = null,
    @SerialName("damage_taken")
    val damageTaken: Int,
    @SerialName("faction_id")
    val factionId: Int? = null,
    @SerialName("items")
    val items: List<ZkillboardItem>? = null,
    @SerialName("position")
    val position: Position? = null,
    @SerialName("ship_type_id")
    val shipTypeId: Int,
)

@Serializable
data class ZkillboardItem(
    @SerialName("flag")
    val flag: Int,
    @SerialName("item_type_id")
    val itemTypeId: Int,
    @SerialName("items")
    val items: List<ZkillboardItem>? = null,
    @SerialName("quantity_destroyed")
    val quantityDestroyed: Int? = null,
    @SerialName("quantity_dropped")
    val quantityDropped: Int? = null,
    @SerialName("singleton")
    val singleton: Int,
)

@Serializable
data class Position(
    @SerialName("x")
    val x: Double,
    @SerialName("y")
    val y: Double,
    @SerialName("z")
    val z: Double,
)

@Serializable
data class Zkb(
    @SerialName("awox")
    val awox: Boolean,
    @SerialName("destroyedValue")
    val destroyedValue: Double,
    @SerialName("droppedValue")
    val droppedValue: Double,
    @SerialName("esi")
    val esi: String,
    @SerialName("fittedValue")
    val fittedValue: Double,
    @SerialName("hash")
    val hash: String,
    @SerialName("locationID")
    val locationID: Int? = null,
    @SerialName("npc")
    val npc: Boolean,
    @SerialName("points")
    val points: Int,
    @SerialName("solo")
    val solo: Boolean,
    @SerialName("totalValue")
    val totalValue: Double,
    @SerialName("url")
    val url: String,
)
