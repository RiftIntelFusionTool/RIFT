package dev.nohus.rift.alerts

import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.planetaryindustry.models.ColonyStatus
import dev.nohus.rift.planetaryindustry.models.PinStatus
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@Single
class PlanetaryInteractionAlertTriggerController(
    private val settings: Settings,
    private val planetaryIndustryRepository: PlanetaryIndustryRepository,
    private val alertsTriggerController: AlertsTriggerController,
) {

    private val mutex = Mutex()

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                checkAlertsExclusive()
            }
        }
        launch {
            planetaryIndustryRepository.colonies.collect {
                checkAlertsExclusive()
            }
        }
        launch {
            settings.updateFlow.map { it.alerts }.collect {
                checkAlertsExclusive()
            }
        }
    }

    private suspend fun checkAlertsExclusive() {
        mutex.withLock {
            checkAlerts()
        }
    }

    private fun checkAlerts() {
        val triggeredAlerts = settings.planetaryIndustryTriggeredAlerts // Alert -> Colony -> Last alerted for timestamp
        val colonies = planetaryIndustryRepository.colonies.value.success?.values ?: emptyList()
        val alerts = settings.alerts.filter { it.isEnabled && it.trigger is AlertTrigger.PlanetaryIndustry }
        val now = Instant.now()
        alerts.map { alert ->
            val triggeredColonies = triggeredAlerts[alert.id] ?: emptyMap() // Colony -> Last alerted for timestamp
            val trigger = alert.trigger as AlertTrigger.PlanetaryIndustry
            val filteredColonies = if (trigger.coloniesFilter == null) {
                colonies
            } else {
                colonies.filter { it.colony.id in trigger.coloniesFilter }
            }
            val triggerBeforeDuration = Duration.ofSeconds(alert.trigger.alertBeforeSeconds.toLong())
            filteredColonies.forEach { colonyItem ->
                val futureStatus = colonyItem.ffwdColony.status
                if (colonyItem.colony.status != futureStatus) {
                    // Status of the colony is going to change in the future
                    if (isStatusMatching(futureStatus, trigger.eventTypes)) {
                        // And it's what the alert is looking for
                        val lastTriggeredForTimestamp = triggeredColonies[colonyItem.colony.id]?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH
                        if (lastTriggeredForTimestamp != colonyItem.ffwdColony.currentSimTime) {
                            // And this event wasn't already alerted for
                            val durationLeft = Duration.between(now, colonyItem.ffwdColony.currentSimTime)
                            val isTriggerTimeReached = durationLeft <= triggerBeforeDuration
                            if (isTriggerTimeReached) {
                                // And we reached the time when the alert should be triggered
                                triggerAlert(alert, colonyItem)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerAlert(
        alert: Alert,
        colonyItem: ColonyItem,
    ) {
        val triggeredAlerts = settings.planetaryIndustryTriggeredAlerts
        val triggeredColonies = triggeredAlerts[alert.id] ?: emptyMap()
        settings.planetaryIndustryTriggeredAlerts = triggeredAlerts + (alert.id to (triggeredColonies + (colonyItem.colony.id to colonyItem.ffwdColony.currentSimTime.toEpochMilli())))
        alertsTriggerController.onNewPlanetaryIndustryAlert(alert, colonyItem)
    }

    private fun isStatusMatching(
        futureStatus: ColonyStatus,
        eventTypes: List<PiEventType>,
    ): Boolean {
        return eventTypes.any { eventType ->
            when (eventType) {
                PiEventType.ExtractorInactive -> {
                    (futureStatus as? ColonyStatus.NeedsAttention)
                        ?.pins
                        ?.any {
                            it.status in listOf(PinStatus.ExtractorInactive, PinStatus.ExtractorExpired)
                        }
                        ?: false
                }

                PiEventType.StorageFull -> {
                    (futureStatus as? ColonyStatus.NeedsAttention)
                        ?.pins
                        ?.any {
                            it.status is PinStatus.StorageFull
                        }
                        ?: false
                }

                PiEventType.Idle -> {
                    futureStatus is ColonyStatus.Idle
                }

                PiEventType.NotSetup -> {
                    futureStatus is ColonyStatus.NotSetup
                }
            }
        }
    }
}
