package dev.nohus.rift.whatsnew

import dev.nohus.rift.whatsnew.WhatsNewViewModel.Point
import dev.nohus.rift.whatsnew.WhatsNewViewModel.Version

object WhatsNew {
    private infix fun String.description(text: String): Version {
        val points = text
            .split("""^-""".toRegex(RegexOption.MULTILINE))
            .mapNotNull { it.takeIf { it.isNotBlank() } }
            .map {
                val isHighlighted = it.startsWith("!")
                Point(
                    text = it.removePrefix("!").trimStart().removeSuffix("\n"),
                    isHighlighted = isHighlighted,
                )
            }
        return Version(
            version = this,
            points = points,
        )
    }

    fun getVersions(): List<Version> {
        return listOf(
            "2.2.0" description """
                -! New feature: Jump bridges!
                - Jump bridges are now shown on the map
                - Your jump bridge network can be either imported from clipboard, or found automatically through ESI. This feature requires a new ESI scope, so you might need to reauthenticate your characters.
                
                - Added button to the About screen to open the app data directory
                - Fixed some issues with the tray icon
                - Fixed some startup issues
            """.trimIndent(),
            "2.3.0" description """
                -! Autopilot route on the map
                - When you set the destination from RIFT, you can now see the autopilot route on the map
                - Added options for setting the autopilot route. You can now select between letting EVE choose the route, or use the route calculated by RIFT.
                
                - Fixed scaling issues with the map on macOS
                - Added warning to the jump bridge search feature
                - Various smaller UI improvements
            """.trimIndent(),
            "2.4.0" description """
                - As you travel along your route on the map, previous systems are now removed from the route
                - Decloaking notification now has an option of ignoring objects you don't want a notification for (like gates)
            """.trimIndent(),
            "2.5.0" description """
                -! Notifications improvements
                - Notifications now have a close button, if you want to get rid of them faster
                - Notifications for Local chat messages will no longer notify for EVE System messages
                - Jabber DM notifications will no longer notify for bot messages
                
                - Added reminder to select configuration pack if you are in a supported alliance but haven't done so
                - Fixed a bunch of bugs
            """.trimIndent(),
            "2.6.0" description """
                -! New feature: Assets!
                - You can now view your assets across all characters
                - Filter, sort and quickly search through your items
                - Copy or view the fittings of your ships
                - Right-click asset locations to view on the map or set autopilot
                - This feature requires a new ESI scope, so you might need to reauthenticate your characters
                
                -! What's new window
                - Added this window, which pops up when the app is updated to let you know of changes
            """.trimIndent(),
        ).reversed()
    }
}
