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
            "2.7.0" description """
                -! New feature: System stats and info on the map!
                - The map can now show the number of jumps, kills, NPC kills, stations, assets you own, incursion status, faction warfare status, and sovereignty
                - Information can be chosen as the system color, or to color to area around systems
                - Details are visible in the info box on hover
                - New collapsible panel on the map allows quickly changing the data to be shown
                - Map has been optimized and consumes less resources
                
                - Assets shown in the assets window now include their prices
                - Mumble is now opened immediately from pings, without going through the web browser
                - When there are multiple EVE installations, the newer character settings directory will be detected
                - Blueprints and skins in the assets window no longer show incorrect icons
            """.trimIndent(),
            "2.8.0" description """
                -! More map information
                - Indicators for selected types of information can now be shown next to systems
                - Selected types of information can now be shown in the system info boxes
                - Metaliminal Storms can now be shown on the map

                -! Assets
                - The total price of items in a location is now visible on the location header
                - Asset location can now be sorted by total price
                - Viewing fits from the assets window now includes the cargo contents
            """.trimIndent(),
            "2.9.0" description """
                -! Jump range on the map
                - The map can now color systems according to jump ranges and show indicators for reachable systems
                - Range can be shown from a specific system, or follow any of your characters
                - You can view the distance in light years for each system
                
                - The assets window will now show the character owning the asset when viewing assets from all characters
            """.trimIndent(),
            "2.10.0" description """
                - Added the ability to lock windows in place
                - Optimized jump bridge search
            """.trimIndent(),
            "2.11.0" description """
                - You can now disable characters that you don't want to use for anything in RIFT
                - The opened region map is now remembered across restarts
            """.trimIndent(),
            "2.12.0" description """
                -! Planets on the map
                - You can now enable map indicators for planets
                - Planet types can be filtered, whether for Skyhook scouting or PI needs
                
                - Made it possible to set up combat alerts with no target filters
                - Added a warning if your EVE client is set to a language other than English
            """.trimIndent(),
            "2.13.0" description """
                - Added EVE Online Partner badge
                - Added prioritisation of ambiguous system names in fleet pings, to choose systems with friendly sovereignty
                - Added support for multiple fleet formup locations in pings
                - Updated formup location distance counter to consider jump bridges
                - Added configuration pack with intel channels for The Initiative.
                - Added support for jump bridge list parsing when copying from Firefox
                - Updated assets browser with new hangar types
            """.trimIndent(),
        ).reversed()
    }
}
