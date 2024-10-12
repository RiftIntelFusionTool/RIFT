package dev.nohus.rift.fleet

import dev.nohus.rift.network.esi.FleetMember
import dev.nohus.rift.network.esi.FleetsId

data class Fleet(
    val id: Long,
    val members: List<FleetMember>,
    val details: FleetsId,
)
