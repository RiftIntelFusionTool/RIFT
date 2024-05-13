package dev.nohus.rift.repositories

import org.koin.core.annotation.Single

@Single
class GetSystemDistanceUseCase(
    private val getRouteUseCase: GetRouteUseCase,
) {

    operator fun invoke(from: Int, to: Int, maxDistance: Int, withJumpBridges: Boolean): Int {
        val route = getRouteUseCase(from, to, maxDistance, withJumpBridges) ?: return Int.MAX_VALUE
        return route.size - 1
    }
}
