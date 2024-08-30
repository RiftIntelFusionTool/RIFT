package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import dev.nohus.rift.autopilot.AutopilotController
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.menu_dotlan
import dev.nohus.rift.generated.resources.menu_everef
import dev.nohus.rift.generated.resources.menu_evewho
import dev.nohus.rift.generated.resources.menu_set_destination
import dev.nohus.rift.generated.resources.menu_uniwiki
import dev.nohus.rift.generated.resources.menu_zkillboard
import dev.nohus.rift.map.MapExternalControl
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.Clipboard
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull

@Composable
fun ClickableLocation(
    systemId: Int,
    locationId: Long?,
    content: @Composable () -> Unit,
) {
    val repository: SolarSystemsRepository = remember { koin.get() }
    val system = repository.getSystemName(systemId) ?: return
    val dotlanUrl = "https://evemaps.dotlan.net/system/$system"
    RiftContextMenuArea(
        items = GetSystemContextMenuItems(systemId, locationId),
    ) {
        ClickableEntity(
            onClick = {
                dotlanUrl.toURIOrNull()?.openBrowser()
            },
            content = content,
        )
    }
}

@Composable
fun ClickableSystem(
    system: String,
    content: @Composable () -> Unit,
) {
    val repository: SolarSystemsRepository = remember { koin.get() }
    val systemId = repository.getSystemId(system) ?: run {
        content()
        return
    }
    val isKnownSpace = repository.isKnownSpace(systemId)
    RiftContextMenuArea(
        items = GetSystemContextMenuItems(systemId),
    ) {
        val mapExternalControl: MapExternalControl = remember { koin.get() }
        ClickableEntity(
            onClick = {
                if (isKnownSpace) {
                    mapExternalControl.showSystemOnRegionMap(systemId, fromMap = false)
                }
            },
            content = content,
        )
    }
}

@Composable
fun GetSystemContextMenuItems(
    systemId: Int?,
    locationId: Long? = null,
    mapType: MapType? = null,
): List<ContextMenuItem> {
    if (systemId == null) return emptyList()

    val autopilotController: AutopilotController = remember { koin.get() }
    val mapExternalControl: MapExternalControl = remember { koin.get() }
    val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
    val settings: Settings = remember { koin.get() }
    val system = solarSystemsRepository.getSystemName(systemId) ?: return emptyList()
    val isKnownSpace = solarSystemsRepository.isKnownSpace(systemId)
    val dotlanUrl = "https://evemaps.dotlan.net/system/$system"
    val zkillboardUrl = "https://zkillboard.com/system/$systemId/"
    var isSettingAutopilotToAll by remember { mutableStateOf(settings.isSettingAutopilotToAll) }
    return buildList {
        add(
            ContextMenuItem.TextItem(
                text = "Set Destination",
                iconResource = Res.drawable.menu_set_destination,
                onClick = {
                    autopilotController.setDestination(locationId ?: systemId.toLong(), systemId)
                },
            ),
        )
        add(
            ContextMenuItem.TextItem(
                text = "Add Waypoint",
                onClick = {
                    autopilotController.addWaypoint(locationId ?: systemId.toLong(), systemId)
                },
            ),
        )
        add(
            ContextMenuItem.TextItem(
                text = "Clear Autopilot",
                onClick = {
                    autopilotController.clearRoute()
                },
            ),
        )
        add(
            ContextMenuItem.CheckboxItem(
                text = "All Characters",
                isSelected = isSettingAutopilotToAll,
                onClick = {
                    isSettingAutopilotToAll = !isSettingAutopilotToAll
                    settings.isSettingAutopilotToAll = isSettingAutopilotToAll
                },
            ),
        )
        add(ContextMenuItem.DividerItem)
        add(
            ContextMenuItem.TextItem(
                text = "Copy Name",
                onClick = {
                    Clipboard.copy(system)
                },
            ),
        )
        if (mapType !is MapType.ClusterSystemsMap && isKnownSpace) {
            add(
                ContextMenuItem.TextItem(
                    text = if (mapType == null) "Show on Map" else "Show in New Eden",
                    onClick = {
                        mapExternalControl.showSystem(systemId, fromMap = mapType != null)
                    },
                ),
            )
        }
        if (mapType !is MapType.RegionMap && isKnownSpace) {
            add(
                ContextMenuItem.TextItem(
                    text = if (mapType == null) "Show on Region Map" else "Show in Region",
                    onClick = {
                        mapExternalControl.showSystemOnRegionMap(systemId, fromMap = mapType != null)
                    },
                ),
            )
        }
        add(ContextMenuItem.DividerItem)
        add(
            ContextMenuItem.TextItem(
                text = "Dotlan",
                iconResource = Res.drawable.menu_dotlan,
                onClick = { dotlanUrl.toURIOrNull()?.openBrowser() },
            ),
        )
        add(
            ContextMenuItem.TextItem(
                text = "zKillboard",
                iconResource = Res.drawable.menu_zkillboard,
                onClick = { zkillboardUrl.toURIOrNull()?.openBrowser() },
            ),
        )
    }
}

@Composable
fun ClickablePlayer(
    characterId: Int?,
    content: @Composable () -> Unit,
) {
    if (characterId == null) {
        content()
        return
    }

    val evewhoUrl = "https://evewho.com/character/$characterId"
    val zKillboardUrl = "https://zkillboard.com/character/$characterId/"
    RiftContextMenuArea(
        listOf(
            ContextMenuItem.TextItem("zKillboard", Res.drawable.menu_zkillboard, onClick = { zKillboardUrl.toURIOrNull()?.openBrowser() }),
            ContextMenuItem.TextItem("EveWho", Res.drawable.menu_evewho, onClick = { evewhoUrl.toURIOrNull()?.openBrowser() }),
        ),
    ) {
        ClickableEntity(
            onClick = {
                zKillboardUrl.toURIOrNull()?.openBrowser()
            },
            content = content,
        )
    }
}

@Composable
fun ClickableCorporation(
    corporationId: Int?,
    content: @Composable () -> Unit,
) {
    if (corporationId == null) {
        content()
        return
    }

    val evewhoUrl = "https://evewho.com/corporation/$corporationId"
    val zKillboardUrl = "https://zkillboard.com/corporation/$corporationId/"
    RiftContextMenuArea(
        listOf(
            ContextMenuItem.TextItem("zKillboard", Res.drawable.menu_zkillboard, onClick = { zKillboardUrl.toURIOrNull()?.openBrowser() }),
            ContextMenuItem.TextItem("EveWho", Res.drawable.menu_evewho, onClick = { evewhoUrl.toURIOrNull()?.openBrowser() }),
        ),
    ) {
        ClickableEntity(
            onClick = {
                zKillboardUrl.toURIOrNull()?.openBrowser()
            },
            content = content,
        )
    }
}

@Composable
fun ClickableAlliance(
    allianceId: Int?,
    content: @Composable () -> Unit,
) {
    if (allianceId == null) {
        content()
        return
    }

    val evewhoUrl = "https://evewho.com/alliance/$allianceId"
    val zKillboardUrl = "https://zkillboard.com/alliance/$allianceId/"
    RiftContextMenuArea(
        listOf(
            ContextMenuItem.TextItem("zKillboard", Res.drawable.menu_zkillboard, onClick = { zKillboardUrl.toURIOrNull()?.openBrowser() }),
            ContextMenuItem.TextItem("EveWho", Res.drawable.menu_evewho, onClick = { evewhoUrl.toURIOrNull()?.openBrowser() }),
        ),
    ) {
        ClickableEntity(
            onClick = {
                zKillboardUrl.toURIOrNull()?.openBrowser()
            },
            content = content,
        )
    }
}

@Composable
fun ClickableShip(
    name: String,
    typeId: Int,
    content: @Composable () -> Unit,
) {
    val uniWikiUrl = "https://wiki.eveuniversity.org/${name.replace(' ', '_')}"
    val eveRefUrl = "https://everef.net/type/$typeId"
    val zKillboard = "https://zkillboard.com/ship/$typeId/"
    RiftContextMenuArea(
        listOf(
            ContextMenuItem.TextItem("UniWiki", Res.drawable.menu_uniwiki, onClick = { uniWikiUrl.toURIOrNull()?.openBrowser() }),
            ContextMenuItem.TextItem("EVE Ref", Res.drawable.menu_everef, onClick = { eveRefUrl.toURIOrNull()?.openBrowser() }),
            ContextMenuItem.TextItem("zKillboard", Res.drawable.menu_zkillboard, onClick = { zKillboard.toURIOrNull()?.openBrowser() }),
        ),
    ) {
        ClickableEntity(
            onClick = {
                uniWikiUrl.toURIOrNull()?.openBrowser()
            },
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickableEntity(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown))
            .onClick { onClick() },
    ) {
        content()
    }
}
