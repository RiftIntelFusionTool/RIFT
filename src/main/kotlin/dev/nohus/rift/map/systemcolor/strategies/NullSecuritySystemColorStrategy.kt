package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.utils.roundSecurity
import org.koin.core.annotation.Single

@Single
class NullSecuritySystemColorStrategy(
    private val solarSystemsRepository: SolarSystemsRepository,
) : SystemColorStrategy() {

    private val activeSecurityColors = mutableMapOf<Double, Color>()

    override fun hasData(system: Int): Boolean {
        val security = solarSystemsRepository.getSystem(system)?.security ?: return false
        return security <= 0.0
    }

    override fun getColor(system: Int): Color {
        val security = solarSystemsRepository.getSystem(system)?.security ?: return Color.Unspecified
        return getActiveSecurityColor(security)
    }

    private fun getActiveSecurityColor(security: Double): Color {
        val rounded = security.roundSecurity()
        return activeSecurityColors.getOrPut(rounded) {
            SecurityColors[(rounded + 1).coerceAtMost(1.0)]
        }
    }
}
