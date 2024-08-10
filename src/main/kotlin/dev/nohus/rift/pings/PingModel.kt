package dev.nohus.rift.pings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed class PingModel {
    abstract val timestamp: Instant
    abstract val sourceText: String

    @Serializable
    @SerialName("PlainText")
    data class PlainText(
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        override val sourceText: String,
        val text: String,
        val sender: String?,
        val target: String?,
    ) : PingModel()

    @Serializable
    @SerialName("FleetPing")
    data class FleetPing(
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        override val sourceText: String,
        val description: String,
        val fleetCommander: FleetCommander,
        val fleet: String?,
        val formupLocations: List<FormupLocation>,
        val papType: PapType?,
        val comms: Comms?,
        val doctrine: Doctrine?,
        val broadcastSource: String?,
        val target: String?,
    ) : PingModel()
}

@Serializable
data class FleetCommander(
    val name: String,
    val id: Int?,
)

@Serializable
sealed interface FormupLocation {
    @Serializable
    data class System(val name: String) : FormupLocation

    @Serializable
    data class Text(val text: String) : FormupLocation
}

@Serializable
sealed interface PapType {
    @Serializable
    data object Strategic : PapType

    @Serializable
    data object Peacetime : PapType

    @Serializable
    data class Text(val text: String) : PapType
}

@Serializable
sealed interface Comms {
    @Serializable
    data class Mumble(
        val channel: String,
        val link: String,
    ) : Comms

    @Serializable
    data class Text(val text: String) : Comms
}

@Serializable
data class Doctrine(
    val text: String,
    val link: String?,
)
