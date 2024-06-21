package dev.nohus.rift.repositories

import org.koin.core.annotation.Single

@Single
class GetSystemsInRangeUseCase(
    private val mapGateConnectionsRepository: MapGateConnectionsRepository,
) {

    operator fun invoke(system: Int, range: Int): Set<Int> {
        return getSystemsInRange(system, emptySet(), range)
    }

    private fun getSystemsInRange(system: Int, ignoredSystems: Set<Int>, range: Int): Set<Int> {
        val neighbors = mapGateConnectionsRepository.systemNeighbors
        if (range == 0) return setOf(system)
        val neighboringSystems = (neighbors[system]?.toSet() ?: emptySet()) - ignoredSystems
        return setOf(system) + neighboringSystems.flatMap {
            getSystemsInRange(it, ignoredSystems + neighboringSystems + system, range - 1)
        }.toSet()
    }
}
