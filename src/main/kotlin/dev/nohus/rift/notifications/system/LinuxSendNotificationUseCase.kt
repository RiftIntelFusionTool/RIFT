package dev.nohus.rift.notifications.system

import dev.nohus.rift.utils.CommandRunner
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class LinuxSendNotificationUseCase(
    private val commandRunner: CommandRunner,
) : SendNotificationUseCase {

    enum class NotificationMethod {
        DBus, NotifySend, KDialog, Zenity
    }

    private val method = detectAvailableNotificationMethod()

    override fun invoke(appName: String, iconPath: String, summary: String, body: String, timeout: Int) {
        try {
            when (method) {
                NotificationMethod.DBus -> sendDBusNotification(appName, iconPath, summary, body, timeout)
                NotificationMethod.NotifySend -> sendNotifySendNotification(appName, iconPath, summary, body, timeout)
                NotificationMethod.KDialog -> sendKDialogNotification(iconPath, summary, body, timeout)
                NotificationMethod.Zenity -> sendZenityNotification(iconPath, summary, body)
                null -> logger.error { "Could not find any binary required to send notifications" }
            }
        } catch (e: IllegalStateException) {
            logger.error { "Could not send notification using $method" }
        }
    }

    private fun detectAvailableNotificationMethod(): NotificationMethod? {
        return when {
            isBinaryFound("gdbus") -> NotificationMethod.DBus
            isBinaryFound("notify-send") -> NotificationMethod.NotifySend
            isBinaryFound("kdialog") -> NotificationMethod.KDialog
            isBinaryFound("zenity") -> NotificationMethod.Zenity
            else -> null
        }
    }

    private fun isBinaryFound(binary: String): Boolean {
        return commandRunner.run("which", binary).exitStatus == 0
    }

    private fun sendDBusNotification(
        appName: String,
        iconPath: String,
        summary: String,
        body: String,
        timeout: Int,
    ) {
        commandRunner.run(
            "gdbus",
            "call",
            "--session",
            "--dest=org.freedesktop.Notifications",
            "--object-path=/org/freedesktop/Notifications",
            "--method=org.freedesktop.Notifications.Notify",
            appName,
            "0",
            iconPath,
            summary,
            body,
            "[]",
            "{\"urgency\": <1>}",
            (timeout * 1000).toString(),
        )
    }

    private fun sendNotifySendNotification(
        appName: String,
        iconPath: String,
        summary: String,
        body: String,
        timeout: Int,
    ) {
        commandRunner.run(
            "notify-send",
            "--app-name=$appName",
            "--icon=$iconPath",
            "--expire-time=${timeout * 1000}",
            summary,
            body,
        )
    }

    private fun sendKDialogNotification(
        iconPath: String,
        summary: String,
        body: String,
        timeout: Int,
    ) {
        commandRunner.run(
            "kdialog",
            "--icon",
            iconPath,
            "--title",
            summary,
            "--passivepopup",
            body,
            timeout.toString(),
        )
    }

    private fun sendZenityNotification(
        iconPath: String,
        summary: String,
        body: String,
    ) {
        commandRunner.run(
            "zenity",
            "--notification",
            "--text=$summary\n$body",
            "--hint=image-path:$iconPath",
        )
    }
}
