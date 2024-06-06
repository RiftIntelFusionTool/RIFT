package dev.nohus.rift.map.systemcolor.strategies

import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategy
import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategyPalettes

class HostileEntitiesSystemColorStrategy(
    private val getIntel: (system: Int) -> List<IntelStateController.Dated<SystemEntity>>,
) : PercentageSystemColorStrategy(PercentageSystemColorStrategyPalettes.negative) {

    override fun hasData(system: Int): Boolean {
        return getIntel(system).isNotEmpty()
    }

    override fun getPercentage(system: Int): Float {
        val data = getIntel(system)
        val entities = data.map { it.item }
        val characterCount = entities.filterIsInstance<SystemEntity.Character>().count() + entities.filterIsInstance<SystemEntity.UnspecifiedCharacter>().sumOf { it.count }
        val shipCount = entities.filterIsInstance<SystemEntity.Ship>().sumOf { it.count }
        var count = maxOf(characterCount, shipCount)
        if (entities.any { it is SystemEntity.GateCamp }) count += 3
        if (entities.any { it is SystemEntity.Spike }) count += 10
        return (count / 5f).coerceIn(0f..1f)
    }
}
