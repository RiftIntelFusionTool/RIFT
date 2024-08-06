package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.koin.core.annotation.Single

@Single
class JoveObservatorySystemColorStrategy(
    private val solarSystemsRepository: SolarSystemsRepository,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return true
    }

    override fun getColor(system: Int): Color {
        val hasJoveObservatory = solarSystemsRepository.getSystem(system)?.hasJoveObservatory ?: return Color.Unspecified
        return if (hasJoveObservatory) Color(0xFF70E552) else Color(0xFFBC1113)
    }
}
