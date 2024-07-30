package dev.nohus.rift.alerts

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class AlertsRepository(
    private val settings: Settings,
) {

    fun add(alert: Alert) {
        val alerts = settings.alerts.filter { it.id != alert.id }
        settings.alerts = alerts + alert
    }

    fun delete(id: String) {
        val alerts = settings.alerts.filter { it.id != id }
        settings.alerts = alerts
    }
}
