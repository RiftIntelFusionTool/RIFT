package dev.nohus.rift.gamelogs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class RecentTargetsRepository {

    private val _targets = MutableStateFlow<Set<String>>(emptySet())
    val targets = _targets.asStateFlow()

    fun onNewGameLogAction(action: GameLogAction?) {
        _targets.value += getCombatTarget(action) ?: return
    }

    private fun getCombatTarget(action: GameLogAction?): String? {
        return when (action) {
            is GameLogAction.Attacking -> action.target
            is GameLogAction.UnderAttack -> action.target
            is GameLogAction.BeingWarpScrambled -> action.target
            else -> null
        }
    }
}
