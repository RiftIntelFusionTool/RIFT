package dev.nohus.rift.pings

import dev.nohus.rift.compose.RiftOpportunityBoxCategory
import dev.nohus.rift.compose.RiftOpportunityBoxCharacter
import java.time.Instant

sealed class PingUiModel(
    open val timestamp: Instant,
    open val sourceText: String,
) {
    data class PlainText(
        override val timestamp: Instant,
        override val sourceText: String,
        val text: String,
        val sender: String?,
        val target: String?,
    ) : PingUiModel(timestamp, sourceText)
    data class FleetPing(
        override val timestamp: Instant,
        override val sourceText: String,
        val opportunityCategory: RiftOpportunityBoxCategory,
        val description: String,
        val fleetCommander: RiftOpportunityBoxCharacter,
        val fleet: String?,
        val formupLocations: List<FormupLocationUiModel>,
        val papType: PapType?,
        val comms: Comms?,
        val doctrine: Doctrine?,
        val target: String?,
    ) : PingUiModel(timestamp, sourceText)
}

sealed interface FormupLocationUiModel {
    data class System(val name: String, val security: Double, val distance: Int) : FormupLocationUiModel
    data class Text(val text: String) : FormupLocationUiModel
}
