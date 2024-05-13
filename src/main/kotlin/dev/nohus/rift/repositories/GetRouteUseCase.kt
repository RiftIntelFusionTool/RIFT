package dev.nohus.rift.repositories

import org.koin.core.annotation.Single

@Single
class GetRouteUseCase(
    private val starGatesRepository: StarGatesRepository,
    private val jumpBridgesRepository: JumpBridgesRepository,
) {
    operator fun invoke(from: Int, to: Int, maxDistance: Int, withJumpBridges: Boolean): List<Int>? {
        val adjacencyMap = getAdjacencyMap(withJumpBridges)
        val distances = mutableMapOf<Int, Int>()
        val previous = mutableMapOf<Int, Int>()
        distances += from to 0
        val unvisited = mutableListOf(from)
        val visited = mutableListOf<Int>()

        while (unvisited.isNotEmpty()) {
            val current = unvisited.removeFirst()
            visited += current
            val distance = distances[current]!!
            if (current == to) {
                val path = mutableListOf(to)
                while (path.last() != from) {
                    path += previous[path.last()]!!
                }
                return path.reversed()
            }
            if (distance > maxDistance) return null // No path within max distance
            val neighbors = adjacencyMap[current] ?: emptyList()
            neighbors
                .filter { it !in visited }
                .forEach { neighbor ->
                    val currentDistance = distances[neighbor] ?: Int.MAX_VALUE
                    if (distance + 1 < currentDistance) {
                        distances += neighbor to distance + 1
                        unvisited += neighbor
                        previous[neighbor] = current
                    }
                }
        }

        return null // No path
    }

    private fun getAdjacencyMap(withJumpBridges: Boolean): Map<Int, List<Int>> {
        val connections = starGatesRepository.connections.toMutableList()
        if (withJumpBridges) {
            val bridgeConnections = jumpBridgesRepository.getConnections()?.map { it.from.id to it.to.id } ?: emptyList()
            connections += bridgeConnections
        }
        return connections
            .groupBy { (from, _) -> from }
            .mapValues { (_, to) -> to.map { it.second } }
    }
}
