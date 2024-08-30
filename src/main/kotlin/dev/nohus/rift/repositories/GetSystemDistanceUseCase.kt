package dev.nohus.rift.repositories

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class GetSystemDistanceUseCase(
    private val getRouteUseCase: GetRouteUseCase,
    private val settings: Settings,
) {

    private data class CacheKey(
        val from: Int,
        val to: Int,
        val maxDistance: Int,
        val withJumpBridges: Boolean,
    )
    private sealed interface CacheValue {
        data object NoRoute : CacheValue
        data class Distance(val distance: Int) : CacheValue
    }
    private val cache: MutableMap<CacheKey, CacheValue> = mutableMapOf()

    private var jumpBridgesHashCode = settings.jumpBridgeNetwork?.hashCode()

    operator fun invoke(from: Int, to: Int, maxDistance: Int, withJumpBridges: Boolean): Int? {
        updateJumpBridgesCode()
        val key = CacheKey(from, to, maxDistance, withJumpBridges)
        cache[key]?.let {
            when (it) {
                is CacheValue.Distance -> return it.distance
                CacheValue.NoRoute -> return null
            }
        }
        val route = getRouteUseCase(from, to, maxDistance, withJumpBridges)?.let { it.size - 1 }
        return route.also {
            cache[key] = if (it != null) CacheValue.Distance(it) else CacheValue.NoRoute
        }
    }

    /**
     * Clears cached distances if the jump bridge networked changed
     */
    private fun updateJumpBridgesCode() {
        val code = settings.jumpBridgeNetwork?.hashCode()
        if (code != jumpBridgesHashCode) {
            jumpBridgesHashCode = code
            cache -= cache.keys.filter { it.withJumpBridges }.toSet()
        }
    }
}
