package dev.nohus.rift.map.systemcolor

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem

interface SolarSystemColorStrategy {
    fun getActiveColor(system: MapSolarSystem): Color
    fun getInactiveColor(system: MapSolarSystem): Color
}
