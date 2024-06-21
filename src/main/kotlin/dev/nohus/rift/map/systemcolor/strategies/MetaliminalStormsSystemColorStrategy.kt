package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormStrength.Strong
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormStrength.Weak
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormType.Electric
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormType.Exotic
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormType.Gamma
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormType.Plasma
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class MetaliminalStormsSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.storms?.takeIf { it.isNotEmpty() } != null
    }

    override fun getColor(system: Int): Color {
        val storms = systemStatus[system]?.storms ?: emptyList()
        return if (storms.size == 1) {
            val storm = storms.single()
            val color = when (storm.type) {
                Gamma -> Color(0xFFFFEE93)
                Electric -> Color(0xFF9AD2E3)
                Plasma -> Color(0xFF9D452D)
                Exotic -> Color(0xFF6DB09E)
            }
            when (storm.strength) {
                Strong -> color
                Weak -> color.copy(alpha = 0.75f).compositeOver(Color.Black)
            }
        } else if (storms.size > 1) {
            val strength = if (storms.any { it.strength == Strong }) Strong else Weak
            when (strength) {
                Strong -> Color(0xFF8F3068)
                Weak -> Color(0xFF8F3068).copy(alpha = 0.75f).compositeOver(Color.Black)
            }
        } else {
            Color.White
        }
    }
}
