package dev.nohus.rift.alerts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    val id: String,
    val trigger: AlertTrigger,
    val actions: List<AlertAction>,
    val isEnabled: Boolean,
    val cooldownSeconds: Int,
    val group: String? = null,
)

@Serializable
sealed interface AlertTrigger {
    @Serializable
    @SerialName("IntelReported")
    data class IntelReported(
        val reportTypes: List<IntelReportType>,
        val reportLocation: IntelReportLocation,
    ) : AlertTrigger

    @Serializable
    @SerialName("GameAction")
    data class GameAction(
        val actionTypes: List<GameActionType>,
    ) : AlertTrigger

    @Serializable
    @SerialName("PlanetaryIndustry")
    data class PlanetaryIndustry(
        val eventTypes: List<PiEventType>,
        val coloniesFilter: List<String>?,
        val alertBeforeSeconds: Int,
    ) : AlertTrigger

    @Serializable
    @SerialName("ChatMessage")
    data class ChatMessage(
        val channel: ChatMessageChannel,
        val sender: String?,
        val messageContaining: String?,
    ) : AlertTrigger

    @Serializable
    @SerialName("JabberMessage")
    data class JabberMessage(
        val channel: JabberMessageChannel,
        val sender: String?,
        val messageContaining: String?,
    ) : AlertTrigger

    @Serializable
    @SerialName("JabberPing")
    data class JabberPing(
        val pingType: JabberPingType,
    ) : AlertTrigger

    @Serializable
    @SerialName("NoChannelActivity")
    data class NoChannelActivity(
        val channel: IntelChannel,
        val durationSeconds: Int,
    ) : AlertTrigger
}

@Serializable
sealed interface IntelReportType {
    @Serializable
    @SerialName("AnyCharacter")
    data object AnyCharacter : IntelReportType

    @Serializable
    @SerialName("SpecificCharacters")
    data class SpecificCharacters(
        val characters: List<String>,
    ) : IntelReportType

    @Serializable
    @SerialName("AnyShip")
    data object AnyShip : IntelReportType

    @Serializable
    @SerialName("SpecificShipClasses")
    data class SpecificShipClasses(
        val classes: List<String>,
    ) : IntelReportType

    @Serializable
    @SerialName("Wormhole")
    data object Wormhole : IntelReportType

    @Serializable
    @SerialName("GateCamp")
    data object GateCamp : IntelReportType

    @Serializable
    @SerialName("Bubbles")
    data object Bubbles : IntelReportType
}

@Serializable
sealed interface IntelReportLocation {
    @Serializable
    @SerialName("System")
    data class System(
        val systemName: String,
        val jumpsRange: JumpRange,
    ) : IntelReportLocation

    @Serializable
    @SerialName("AnyOwnedCharacter")
    data class AnyOwnedCharacter(
        val jumpsRange: JumpRange,
    ) : IntelReportLocation

    @Serializable
    @SerialName("OwnedCharacter")
    data class OwnedCharacter(
        val characterId: Int,
        val jumpsRange: JumpRange,
    ) : IntelReportLocation
}

@Serializable
data class JumpRange(
    val min: Int,
    val max: Int,
)

@Serializable
sealed interface GameActionType {
    @Serializable
    @SerialName("InCombat")
    data class InCombat(
        val nameContaining: String?,
    ) : GameActionType

    @Serializable
    @SerialName("UnderAttack")
    data class UnderAttack(
        val nameContaining: String?,
    ) : GameActionType

    @Serializable
    @SerialName("Attacking")
    data class Attacking(
        val nameContaining: String?,
    ) : GameActionType

    @Serializable
    @SerialName("BeingWarpScrambled")
    data object BeingWarpScrambled : GameActionType

    @Serializable
    @SerialName("Decloaked")
    data class Decloaked(
        val ignoredKeywords: List<String> = emptyList(),
    ) : GameActionType

    @Serializable
    @SerialName("CombatStopped")
    data class CombatStopped(
        val nameContaining: String?,
        val durationSeconds: Int,
    ) : GameActionType
}

@Serializable
sealed interface PiEventType {
    @Serializable
    @SerialName("NotSetup")
    data object NotSetup : PiEventType

    @Serializable
    @SerialName("ExtractorInactive")
    data object ExtractorInactive : PiEventType

    @Serializable
    @SerialName("StorageFull")
    data object StorageFull : PiEventType

    @Serializable
    @SerialName("Idle")
    data object Idle : PiEventType
}

@Serializable
sealed interface IntelChannel {
    @Serializable
    @SerialName("All")
    data object All : IntelChannel

    @Serializable
    @SerialName("Any")
    data object Any : IntelChannel

    @Serializable
    @SerialName("Channel")
    data class Channel(val name: String) : IntelChannel
}

@Serializable
sealed interface ChatMessageChannel {
    @Serializable
    @SerialName("Any")
    data object Any : ChatMessageChannel

    @Serializable
    @SerialName("Channel")
    data class Channel(val name: String) : ChatMessageChannel
}

@Serializable
sealed interface JabberMessageChannel {
    @Serializable
    @SerialName("Any")
    data object Any : JabberMessageChannel

    @Serializable
    @SerialName("Channel")
    data class Channel(val name: String) : JabberMessageChannel

    @Serializable
    @SerialName("DirectMessage")
    data object DirectMessage : JabberMessageChannel
}

@Serializable
sealed interface JabberPingType {
    @Deprecated("Removed")
    @Serializable
    @SerialName("Message")
    data object Message : JabberPingType

    @Serializable
    @SerialName("Message2")
    data class Message2(
        val target: String?,
    ) : JabberPingType

    @Serializable
    @SerialName("Fleet")
    data class Fleet(
        val fleetCommanders: List<String>,
        val formupSystem: String?,
        val papType: PapType,
        val doctrineContaining: String?,
        val target: String? = null,
    ) : JabberPingType
}

@Serializable
sealed interface PapType {
    @Serializable
    @SerialName("Strategic")
    data object Strategic : PapType

    @Serializable
    @SerialName("Peacetime")
    data object Peacetime : PapType

    @Serializable
    @SerialName("Any")
    data object Any : PapType
}

@Serializable
sealed interface AlertAction {
    @Serializable
    @SerialName("RiftNotification")
    data object RiftNotification : AlertAction

    @Serializable
    @SerialName("SystemNotification")
    data object SystemNotification : AlertAction

    @Serializable
    @SerialName("PushNotification")
    data object PushNotification : AlertAction

    @Serializable
    @SerialName("Sound")
    data class Sound(
        val id: Int,
    ) : AlertAction

    @Serializable
    @SerialName("CustomSound")
    data class CustomSound(
        val path: String,
    ) : AlertAction

    @Serializable
    @SerialName("ShowPing")
    data object ShowPing : AlertAction

    @Serializable
    @SerialName("ShowColonies")
    data object ShowColonies : AlertAction
}
