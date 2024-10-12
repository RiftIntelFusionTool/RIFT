package dev.nohus.rift.map.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.map.settings.MapSettingsViewModel.JumpBridgeNetworkState
import dev.nohus.rift.map.settings.MapSettingsViewModel.UiState
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun MapSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: MapSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Map Settings",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        MapSettingsWindowContent(
            state = state,
            onIntelPopupTimeoutSecondsChange = viewModel::onIntelPopupTimeoutSecondsChange,
            onIsUsingCompactModeChange = viewModel::onIsUsingCompactModeChange,
            onIsCharacterFollowingChange = viewModel::onIsCharacterFollowingChange,
            onIsScrollZoomInvertedChange = viewModel::onIsScrollZoomInvertedChange,
            onIsUsingRiftAutopilotRouteChange = viewModel::onIsUsingRiftAutopilotRouteChange,
            onIsJumpBridgeNetworkShownChange = viewModel::onIsJumpBridgeNetworkShownChange,
            onJumpBridgeNetworkOpacityChange = viewModel::onJumpBridgeNetworkOpacityChange,
            onJumpBridgeForgetClick = viewModel::onJumpBridgeForgetClick,
            onJumpBridgeImportClick = viewModel::onJumpBridgeImportClick,
            onJumpBridgeSearchClick = viewModel::onJumpBridgeSearchClick,
            onJumpBridgeSearchImportClick = viewModel::onJumpBridgeSearchImportClick,
        )

        if (state.isJumpBridgeSearchDialogShown) {
            RiftDialog(
                title = "Jump Bridge Search",
                icon = Res.drawable.window_warning,
                parentState = windowState,
                state = rememberWindowState(width = 350.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onDialogDismissed,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Text(
                        text = "This feature is not unique to RIFT, and no problems were reported with it, but some " +
                            "concerns were raised that it might trip ESI's hidden rate limits and block your IP address.",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Text(
                        text = "Use at your own risk!",
                        textAlign = TextAlign.Center,
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        RiftButton(
                            text = "Cancel",
                            cornerCut = ButtonCornerCut.BottomLeft,
                            type = ButtonType.Secondary,
                            onClick = viewModel::onDialogDismissed,
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "Confirm",
                            type = ButtonType.Secondary,
                            onClick = viewModel::onJumpBridgeSearchDialogConfirmClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapSettingsWindowContent(
    state: UiState,
    onIntelPopupTimeoutSecondsChange: (Int) -> Unit,
    onIsUsingCompactModeChange: (Boolean) -> Unit,
    onIsCharacterFollowingChange: (Boolean) -> Unit,
    onIsScrollZoomInvertedChange: (Boolean) -> Unit,
    onIsUsingRiftAutopilotRouteChange: (Boolean) -> Unit,
    onIsJumpBridgeNetworkShownChange: (Boolean) -> Unit,
    onJumpBridgeNetworkOpacityChange: (Int) -> Unit,
    onJumpBridgeForgetClick: () -> Unit,
    onJumpBridgeImportClick: () -> Unit,
    onJumpBridgeSearchClick: () -> Unit,
    onJumpBridgeSearchImportClick: () -> Unit,
) {
    val intelMap = state.intelMap
    ScrollbarColumn {
        SectionTitle("User interface", Modifier.padding(bottom = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftCheckboxWithLabel(
                label = "Compact mode",
                isChecked = intelMap.isUsingCompactMode,
                onCheckedChange = onIsUsingCompactModeChange,
            )
            RiftCheckboxWithLabel(
                label = "Move with character",
                tooltip = "When you jump to another system\nthe map will follow",
                isChecked = intelMap.isCharacterFollowing,
                onCheckedChange = { onIsCharacterFollowingChange(it) },
            )
            RiftCheckboxWithLabel(
                label = "Invert scroll wheel zoom",
                tooltip = "Zoom direction will be reversed",
                isChecked = intelMap.isInvertZoom,
                onCheckedChange = { onIsScrollZoomInvertedChange(it) },
            )
            Text(
                text = buildAnnotatedString {
                    withColor(RiftTheme.colors.textPrimary) {
                        append("Tip:")
                    }
                    append(" Press Space on the map to automatically resize")
                },
                style = RiftTheme.typography.bodySecondary,
            )
        }

        SectionTitle("Autopilot", Modifier.padding(vertical = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(
                text = "When setting autopilot destination, use:",
                style = RiftTheme.typography.bodySecondary,
            )
            RiftRadioButtonWithLabel(
                label = "RIFT calculated route",
                tooltip = "Shortest route as shown on the RIFT map.\nIgnores your EVE autopilot settings.",
                isChecked = state.isUsingRiftAutopilotRoute,
                onChecked = { onIsUsingRiftAutopilotRouteChange(true) },
            )
            RiftRadioButtonWithLabel(
                label = "EVE calculated route",
                tooltip = "Route as set by EVE.\nMay not match the route on the RIFT map.",
                isChecked = !state.isUsingRiftAutopilotRoute,
                onChecked = { onIsUsingRiftAutopilotRouteChange(false) },
            )
        }

        SectionTitle("Jump bridge network", Modifier.padding(top = Spacing.medium))
        Column {
            AnimatedContent(state.jumpBridgeNetworkState) { networkState ->
                when (networkState) {
                    JumpBridgeNetworkState.Empty -> {
                        // Empty
                    }
                    is JumpBridgeNetworkState.Loaded -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(top = Spacing.medium, end = Spacing.medium).fillMaxWidth(),
                        ) {
                            RiftTooltipArea(
                                text = "Import jump bridges by copying a list to clipboard",
                            ) {
                                Text("Network with ${networkState.network.connections.size} connections loaded")
                            }
                            RiftButton(
                                text = "Forget",
                                type = ButtonType.Negative,
                                onClick = onJumpBridgeForgetClick,
                            )
                        }
                    }
                }
            }
            AnimatedContent(state.jumpBridgeCopyState) { copyState ->
                when (copyState) {
                    MapSettingsViewModel.JumpBridgeCopyState.NotCopied -> {
                        if (state.jumpBridgeNetworkState == JumpBridgeNetworkState.Empty) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier.padding(top = Spacing.medium),
                            ) {
                                Text("Import jump bridges by copying a list to clipboard")
                                if (state.jumpBridgeNetworkUrl != null) {
                                    Text("You can press Ctrl+A, Ctrl+C on this page:")
                                    LinkText(
                                        text = "Alliance Jump Bridge List",
                                        onClick = { state.jumpBridgeNetworkUrl.toURIOrNull()?.openBrowser() },
                                    )
                                }
                            }
                        }
                    }
                    is MapSettingsViewModel.JumpBridgeCopyState.Copied -> {
                        val tooltip = buildString {
                            val connections = copyState.network.connections.take(5).joinToString("\n") {
                                "${it.from.name} → ${it.to.name}"
                            }
                            append(connections)
                            if (copyState.network.connections.size > 5) {
                                appendLine()
                                append("And more…")
                            }
                        }
                        RiftTooltipArea(
                            text = tooltip,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(top = Spacing.medium, end = Spacing.medium).fillMaxWidth(),
                            ) {
                                Text("Copied network with ${copyState.network.connections.size} connections")
                                RiftButton(
                                    text = "Import",
                                    onClick = onJumpBridgeImportClick,
                                )
                            }
                        }
                    }
                }
            }
            AnimatedContent(state.jumpBridgeSearchState, contentKey = { it::class }) { searchState ->
                when (searchState) {
                    MapSettingsViewModel.JumpBridgeSearchState.NotSearched -> {
                        if (state.jumpBridgeNetworkState == JumpBridgeNetworkState.Empty) {
                            Column(
                                modifier = Modifier.padding(top = Spacing.medium),
                            ) {
                                Divider(
                                    color = RiftTheme.colors.divider,
                                    modifier = Modifier.padding(bottom = Spacing.medium),
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
                                ) {
                                    Text("Search automatically?")
                                    RiftButton(
                                        text = "Search",
                                        onClick = onJumpBridgeSearchClick,
                                    )
                                }
                            }
                        }
                    }
                    is MapSettingsViewModel.JumpBridgeSearchState.Searching -> {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Divider(
                                color = RiftTheme.colors.divider,
                                modifier = Modifier.padding(bottom = Spacing.medium),
                            )
                            Text("Searching – ${String.format("%.1f", searchState.progress * 100)}%")
                            Text(
                                text = "Found ${searchState.connectionsCount} jump gate connections",
                                style = RiftTheme.typography.bodySecondary,
                            )
                        }
                    }
                    MapSettingsViewModel.JumpBridgeSearchState.SearchFailed -> {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Divider(
                                color = RiftTheme.colors.divider,
                                modifier = Modifier.padding(bottom = Spacing.medium),
                            )
                            Text("Unable to search")
                        }
                    }
                    is MapSettingsViewModel.JumpBridgeSearchState.SearchDone -> {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Divider(
                                color = RiftTheme.colors.divider,
                                modifier = Modifier.padding(bottom = Spacing.medium),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
                            ) {
                                Text("Found network with ${searchState.network.connections.size} connections")
                                RiftButton(
                                    text = "Import",
                                    onClick = onJumpBridgeSearchImportClick,
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(state.jumpBridgeNetworkState is JumpBridgeNetworkState.Loaded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(top = Spacing.medium),
                ) {
                    RiftCheckboxWithLabel(
                        label = "Show network on map",
                        isChecked = intelMap.isJumpBridgeNetworkShown,
                        onCheckedChange = onIsJumpBridgeNetworkShownChange,
                    )
                    val jumpBridgeOpacityItems = mapOf(
                        "10%" to 10,
                        "25%" to 25,
                        "50%" to 50,
                        "75%" to 75,
                        "100%" to 100,
                    )
                    RiftDropdownWithLabel(
                        label = "Connection opacity:",
                        items = jumpBridgeOpacityItems.values.toList(),
                        selectedItem = intelMap.jumpBridgeNetworkOpacity,
                        onItemSelected = onJumpBridgeNetworkOpacityChange,
                        getItemName = { item -> jumpBridgeOpacityItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
                        tooltip = """
                    Visibility of the jump bridge
                    connection lines.
                        """.trimIndent(),
                    )
                }
            }
        }

        SectionTitle("Intel visibility", Modifier.padding(vertical = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            val timeoutItems = mapOf(
                "10 seconds" to 10,
                "30 seconds" to 30,
                "1 minute" to 60,
                "2 minutes" to 60 * 2,
                "5 minutes" to 60 * 5,
                "15 minutes" to 60 * 15,
                "No limit" to Int.MAX_VALUE,
            )
            RiftDropdownWithLabel(
                label = "Automatically show popups for:",
                items = timeoutItems.values.toList(),
                selectedItem = intelMap.intelPopupTimeoutSeconds,
                onItemSelected = onIntelPopupTimeoutSecondsChange,
                getItemName = { item -> timeoutItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
                tooltip = """
                    For how long will intel popups be visible
                    when new information is available.
                    They are visible on hover even after this time.
                """.trimIndent(),
            )
        }
    }
}
