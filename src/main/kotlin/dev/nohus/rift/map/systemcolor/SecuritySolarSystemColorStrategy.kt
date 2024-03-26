package dev.nohus.rift.map.systemcolor

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.utils.roundSecurity

class SecuritySolarSystemColorStrategy : SolarSystemColorStrategy {

    private val inactiveSecurityColors = mutableMapOf<Double, Color>()
    private val activeSecurityColors = mutableMapOf<Double, Color>()

    override fun getActiveColor(system: MapSolarSystem): Color {
        return getActiveSecurityColor(system.security)
    }

    override fun getInactiveColor(system: MapSolarSystem): Color {
        return getInactiveSecurityColor(system.security)
    }

    private fun getInactiveSecurityColor(security: Double): Color {
        val rounded = security.roundSecurity()
        return inactiveSecurityColors.getOrPut(rounded) {
            SecurityColors[rounded].copy(alpha = 0.1f)
        }
    }

    private fun getActiveSecurityColor(security: Double): Color {
        val rounded = security.roundSecurity()
        return activeSecurityColors.getOrPut(rounded) {
            SecurityColors[rounded]
        }
    }
}
