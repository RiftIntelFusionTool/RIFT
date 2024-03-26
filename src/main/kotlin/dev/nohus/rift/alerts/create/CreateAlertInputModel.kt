package dev.nohus.rift.alerts.create

import dev.nohus.rift.alerts.Alert

sealed interface CreateAlertInputModel {
    data object New : CreateAlertInputModel
    data class EditAction(val alert: Alert) : CreateAlertInputModel
}
