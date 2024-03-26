package dev.nohus.rift.utils.activewindow

import dev.nohus.rift.utils.CommandRunner

class LinuxGetActiveWindowUseCase(
    private val commandRunner: CommandRunner,
) : GetActiveWindowUseCase {

    private val activeWindowRegex = """ # (?<id>0x[0-9a-f]+)""".toRegex()
    private val windowNameRegex = """ = "(?<name>.*)"""".toRegex()

    override fun invoke(): String? {
        val id = getActiveWindowId() ?: return null
        return getWindowName(id)
    }

    private fun getActiveWindowId(): String? {
        val result = commandRunner.run("xprop", "-root", "_NET_ACTIVE_WINDOW", ignoreFailure = true)
        return if (result.exitStatus == 0) {
            activeWindowRegex.find(result.output)?.groups?.get("id")?.value
        } else {
            null
        }
    }

    private fun getWindowName(id: String): String? {
        val result = commandRunner.run("xprop", "-id", id, "_NET_WM_NAME", ignoreFailure = true)
        return if (result.exitStatus == 0) {
            windowNameRegex.find(result.output)?.groups?.get("name")?.value
        } else {
            null
        }
    }
}
