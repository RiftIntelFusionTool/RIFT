package dev.nohus.rift.repositories

import org.koin.core.annotation.Single

@Single
class GetSystemDistanceUseCase(
    starGatesRepository: StarGatesRepository,
) {

    private val neighbors = starGatesRepository.connections
        .groupBy { (from, _) -> from }
        .mapValues { (_, to) -> to.map { it.second } }
    private var cache = mutableMapOf<Pair<Int, Int>, Int>()

    operator fun invoke(from: Int, to: Int, maxDistance: Int): Int {
        cache[from to to]?.let { return it }

        val distances = mutableMapOf<Int, Int>()
        distances += from to 0
        val unvisited = mutableListOf(from)
        val visited = mutableListOf<Int>()

        while (unvisited.isNotEmpty()) {
            val current = unvisited.removeFirst()
            visited += current
            val distance = distances[current]!!
            if (current == to) {
                return distance.also {
                    cache[from to to] = it
                    cache[to to from] = it
                }
            }
            if (distance > maxDistance) return Int.MAX_VALUE // No path within max distance
            val neighbors = neighbors[current] ?: emptyList()
            neighbors
                .filter { it !in visited }
                .forEach { neighbor ->
                    distances += neighbor to (minOf(distances[neighbor] ?: Int.MAX_VALUE, distance + 1))
                    unvisited += neighbor
                }
        }

        return Int.MAX_VALUE // No path
    }
}
