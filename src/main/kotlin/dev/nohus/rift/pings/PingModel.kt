package dev.nohus.rift.pings

import java.time.Instant

sealed class PingModel(
    open val timestamp: Instant,
    open val sourceText: String,
) {
    data class PlainText(
        override val timestamp: Instant,
        override val sourceText: String,
        val text: String,
        val sender: String?,
        val target: String?,
    ) : PingModel(timestamp, sourceText)
    data class FleetPing(
        override val timestamp: Instant,
        override val sourceText: String,
        val description: String,
        val fleetCommander: FleetCommander,
        val fleet: String?,
        val formupSystem: FormupLocation?,
        val papType: PapType?,
        val comms: Comms?,
        val doctrine: Doctrine?,
        val broadcastSource: String?,
        val target: String?,
    ) : PingModel(timestamp, sourceText)
}

data class FleetCommander(
    val name: String,
    val id: Int?,
)

sealed interface FormupLocation {
    data class System(val name: String) : FormupLocation
    data class Text(val text: String) : FormupLocation
}

sealed interface PapType {
    data object Strategic : PapType
    data object Peacetime : PapType
    data class Text(val text: String) : PapType
}

sealed interface Comms {
    data class Mumble(
        val channel: String,
        val link: String,
    ) : Comms
    data class Text(val text: String) : Comms
}

data class Doctrine(
    val text: String,
    val link: String?,
)
