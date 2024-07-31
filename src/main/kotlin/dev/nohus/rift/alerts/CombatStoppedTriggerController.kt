package dev.nohus.rift.alerts

import dev.nohus.rift.gamelogs.GameLogAction
import java.time.Duration
import java.time.Instant

class CombatStoppedTriggerController(
    private val onTrigger: (alert: Alert, action: GameLogAction, characterId: Int) -> Unit,
) {

    data class TriggerInfo(
        val triggerTimestamp: Instant,
        val target: String,
        val characterId: Int,
    )

    private val pendingAlerts: MutableMap<Alert, TriggerInfo> = mutableMapOf()

    fun onCombatAction(alert: Alert, durationSeconds: Int, target: String, characterId: Int) {
        val triggerInfo = TriggerInfo(
            triggerTimestamp = Instant.now() + Duration.ofSeconds(durationSeconds.toLong()),
            target = target,
            characterId = characterId,
        )
        pendingAlerts[alert] = triggerInfo
    }

    fun checkPendingAlerts() {
        if (pendingAlerts.isEmpty()) return

        val triggeredAlerts = pendingAlerts.filter { it.value.triggerTimestamp.isBefore(Instant.now()) }
        pendingAlerts -= triggeredAlerts.keys
        triggeredAlerts.forEach { (triggeredAlert, triggerInfo) ->
            onTrigger(triggeredAlert, GameLogAction.CombatStopped(triggerInfo.target), triggerInfo.characterId)
        }
    }
}
