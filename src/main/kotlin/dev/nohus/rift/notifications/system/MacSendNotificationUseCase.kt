package dev.nohus.rift.notifications.system

import dev.nohus.rift.utils.CommandRunner

class MacSendNotificationUseCase(
    private val commandRunner: CommandRunner,
) : SendNotificationUseCase {

    override fun invoke(appName: String, iconPath: String, summary: String, body: String, timeout: Int) {
        commandRunner.run(
            "osascript",
            "-e",
            """display notification "$body" with title "$appName" subtitle "$summary"""",
        )
    }
}
