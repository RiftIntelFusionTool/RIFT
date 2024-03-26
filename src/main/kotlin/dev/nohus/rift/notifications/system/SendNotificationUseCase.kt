package dev.nohus.rift.notifications.system

interface SendNotificationUseCase {
    operator fun invoke(
        appName: String,
        iconPath: String,
        summary: String,
        body: String,
        timeout: Int,
    )
}
