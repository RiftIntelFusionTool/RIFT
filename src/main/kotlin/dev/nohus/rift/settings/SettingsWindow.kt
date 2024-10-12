package dev.nohus.rift.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftFileChooserButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.ConfigurationPackRepository.SuggestedIntelChannels
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.notifications.NotificationEditWindow
import dev.nohus.rift.settings.SettingsViewModel.UiState
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import javax.swing.JFileChooser
import kotlin.io.path.absolutePathString

@Composable
fun SettingsWindow(
    inputModel: SettingsInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "RIFT Settings",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        SettingsWindowContent(
            inputModel = inputModel,
            state = state,
            viewModel = viewModel,
            onDoneClick = onCloseRequest,
        )

        if (state.isEditNotificationWindowOpen) {
            NotificationEditWindow(
                position = state.notificationEditPlacement,
                onCloseRequest = viewModel::onEditNotificationDone,
            )
        }

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }
    }
}

@Composable
private fun SettingsWindowContent(
    inputModel: SettingsInputModel,
    state: UiState,
    viewModel: SettingsViewModel,
    onDoneClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.height(IntrinsicSize.Max),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.weight(1f),
        ) {
            SectionContainer(inputModel, SettingsInputModel.IntelChannels) {
                IntelChannelsSection(
                    intelChannels = state.intelChannels,
                    onIntelChannelDelete = viewModel::onIntelChannelDelete,
                    regions = state.regions,
                    suggestedIntelChannels = state.suggestedIntelChannels,
                    onSuggestedIntelChannelsClick = viewModel::onSuggestedIntelChannelsClick,
                    onIntelChannelAdded = viewModel::onIntelChannelAdded,
                )
            }

            SectionContainer(inputModel) {
                IntelSection(
                    isShowingSystemDistance = state.isShowingSystemDistance,
                    onIsShowingSystemDistanceChange = viewModel::onIsShowingSystemDistanceChange,
                    isUsingJumpBridgesForDistance = state.isUsingJumpBridgesForDistance,
                    onIsUsingJumpBridgesForDistance = viewModel::onIsUsingJumpBridgesForDistance,
                    intelExpireSeconds = state.intelExpireSeconds,
                    onIntelExpireSecondsChange = viewModel::onIntelExpireSecondsChange,
                )
            }

            SectionContainer(inputModel, SettingsInputModel.EveInstallation) {
                EveInstallationSection(
                    isLogsDirectoryValid = state.isLogsDirectoryValid,
                    logsDirectory = state.logsDirectory,
                    onLogsDirectoryChanged = viewModel::onLogsDirectoryChanged,
                    onDetectLogsDirectoryClick = viewModel::onDetectLogsDirectoryClick,
                    isSettingsDirectoryValid = state.isSettingsDirectoryValid,
                    settingsDirectory = state.settingsDirectory,
                    onSettingsDirectoryChanged = viewModel::onSettingsDirectoryChanged,
                    onDetectSettingsDirectoryClick = viewModel::onDetectSettingsDirectoryClick,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.weight(1f),
        ) {
            SectionContainer(inputModel) {
                UserInterfaceSection(
                    isRememberOpenWindowsEnabled = state.isRememberOpenWindows,
                    onRememberOpenWindowsChanged = viewModel::onRememberOpenWindowsChanged,
                    isRememberWindowPlacementEnabled = state.isRememberWindowPlacement,
                    onRememberWindowPlacementChanged = viewModel::onRememberWindowPlacementChanged,
                    isDisplayEveTime = state.isDisplayEveTime,
                    onIsDisplayEveTimeChanged = viewModel::onIsDisplayEveTimeChanged,
                    isUsingDarkTrayIcon = state.isUsingDarkTrayIcon,
                    onIsUsingDarkTrayIconChanged = viewModel::onIsUsingDarkTrayIconChanged,
                )
            }

            SectionContainer(inputModel) {
                AlertsSection(
                    soundsVolume = state.soundsVolume,
                    onSoundsVolumeChange = viewModel::onSoundsVolumeChange,
                    onEditNotificationClick = viewModel::onEditNotificationClick,
                    onConfigurePushoverClick = viewModel::onConfigurePushoverClick,
                )
            }

            SectionContainer(inputModel) {
                OtherSettingsSection(
                    configurationPack = state.configurationPack,
                    onConfigurationPackChange = viewModel::onConfigurationPackChange,
                    isLoadOldMessagesEnabled = state.isLoadOldMessagesEnabled,
                    onLoadOldMessagesChanged = viewModel::onLoadOldMessagedChanged,
                    isShowSetupWizardOnNextStartEnabled = state.isShowSetupWizardOnNextStartEnabled,
                    onShowSetupWizardOnNextStartChanged = viewModel::onShowSetupWizardOnNextStartChanged,
                )
            }

            Spacer(Modifier.weight(1f))
            RiftButton(
                text = "Done",
                onClick = onDoneClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = Spacing.medium),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionContainer(
    inputModel: SettingsInputModel,
    enabledInputModel: SettingsInputModel? = null,
    content: @Composable () -> Unit,
) {
    val isEnabled = inputModel == SettingsInputModel.Normal || inputModel == enabledInputModel
    Box(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .modifyIf(!isEnabled) {
                alpha(0.3f)
            },
    ) {
        Column {
            content()
        }
        if (!isEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onClick {},
            ) {}
        }
    }
}

@Composable
private fun UserInterfaceSection(
    isRememberOpenWindowsEnabled: Boolean,
    onRememberOpenWindowsChanged: (Boolean) -> Unit,
    isRememberWindowPlacementEnabled: Boolean,
    onRememberWindowPlacementChanged: (Boolean) -> Unit,
    isDisplayEveTime: Boolean,
    onIsDisplayEveTimeChanged: (Boolean) -> Unit,
    isUsingDarkTrayIcon: Boolean,
    onIsUsingDarkTrayIconChanged: (Boolean) -> Unit,
) {
    SectionTitle("User Interface", Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = "Remember open windows",
        tooltip = "Enable to remember open windows\nacross app restarts",
        isChecked = isRememberOpenWindowsEnabled,
        onCheckedChange = onRememberOpenWindowsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Remember window placement",
        tooltip = "Enable to remember window positions and\nsizes across app restarts",
        isChecked = isRememberWindowPlacementEnabled,
        onCheckedChange = onRememberWindowPlacementChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Display times in EVE time",
        tooltip = "Enable to show times in EVE time,\ninstead of your own time zone.",
        isChecked = isDisplayEveTime,
        onCheckedChange = onIsDisplayEveTimeChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Use dark tray icon",
        tooltip = "Enable to use a dark tray icon,\nif you prefer it.",
        isChecked = isUsingDarkTrayIcon,
        onCheckedChange = onIsUsingDarkTrayIconChanged,
    )
}

@Composable
private fun AlertsSection(
    soundsVolume: Int,
    onSoundsVolumeChange: (Int) -> Unit,
    onEditNotificationClick: () -> Unit,
    onConfigurePushoverClick: () -> Unit,
) {
    SectionTitle("Alerts", Modifier.padding(bottom = Spacing.medium))
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Choose notification position:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "Edit position",
            onClick = onEditNotificationClick,
        )
    }
    RiftDropdownWithLabel(
        label = "Alert volume:",
        items = (0..100 step 10).reversed().toList(),
        selectedItem = soundsVolume,
        onItemSelected = onSoundsVolumeChange,
        getItemName = { "$it%" },
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mobile push notifications:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "Configure",
            onClick = onConfigurePushoverClick,
        )
    }
}

@Composable
private fun OtherSettingsSection(
    configurationPack: ConfigurationPack?,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    isLoadOldMessagesEnabled: Boolean,
    onLoadOldMessagesChanged: (Boolean) -> Unit,
    isShowSetupWizardOnNextStartEnabled: Boolean,
    onShowSetupWizardOnNextStartChanged: (Boolean) -> Unit,
) {
    SectionTitle("Advanced Settings", Modifier.padding(bottom = Spacing.medium))
    RiftDropdownWithLabel(
        label = "Configuration pack:",
        items = listOf(null) + ConfigurationPack.entries,
        selectedItem = configurationPack,
        onItemSelected = onConfigurationPackChange,
        getItemName = { it?.displayName ?: "Default" },
        tooltip = """
            Enables settings specific to a player group,
            like intel channel suggestions.
            Contact me on Discord if you'd like to add yours.
        """.trimIndent(),
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Load old chat messages",
        tooltip = "Enable to read old chat logs,\ninstead of only new messages.\nNot recommended.",
        isChecked = isLoadOldMessagesEnabled,
        onCheckedChange = onLoadOldMessagesChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Show setup wizard on next start",
        tooltip = "Did you know that Aura is a wizard?",
        isChecked = isShowSetupWizardOnNextStartEnabled,
        onCheckedChange = onShowSetupWizardOnNextStartChanged,
    )
}

@Composable
private fun EveInstallationSection(
    isLogsDirectoryValid: Boolean,
    logsDirectory: String,
    onLogsDirectoryChanged: (String) -> Unit,
    onDetectLogsDirectoryClick: () -> Unit,
    isSettingsDirectoryValid: Boolean,
    settingsDirectory: String,
    onSettingsDirectoryChanged: (String) -> Unit,
    onDetectSettingsDirectoryClick: () -> Unit,
) {
    SectionTitle("EVE Installation")
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE Online logs directory",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = isLogsDirectoryValid,
            fulfilledTooltip = "Logs directory valid",
            notFulfilledTooltip = if (logsDirectory.isBlank()) "No logs directory" else "Invalid logs directory",
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(logsDirectory) { mutableStateOf(logsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                onLogsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = "Chat logs directory",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                onLogsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "Detect",
            type = if (isLogsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = onDetectLogsDirectoryClick,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE Online character settings directory",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = isSettingsDirectoryValid,
            fulfilledTooltip = "Settings directory valid",
            notFulfilledTooltip = if (settingsDirectory.isBlank()) "No settings directory" else "Invalid settings directory",
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(settingsDirectory) { mutableStateOf(settingsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                onSettingsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = "Game logs directory",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                onSettingsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "Detect",
            type = if (isSettingsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = onDetectSettingsDirectoryClick,
        )
    }
}

@Composable
private fun IntelChannelsSection(
    intelChannels: List<IntelChannel>,
    onIntelChannelDelete: (IntelChannel) -> Unit,
    regions: List<String>,
    suggestedIntelChannels: SuggestedIntelChannels?,
    onSuggestedIntelChannelsClick: () -> Unit,
    onIntelChannelAdded: (name: String, region: String) -> Unit,
) {
    SectionTitle("Intel Channels")
    Text(
        text = "Intel reports will be read from these channels:",
        style = RiftTheme.typography.bodyPrimary,
        modifier = Modifier.padding(vertical = Spacing.medium),
    )
    ScrollbarColumn(
        modifier = Modifier
            .height(140.dp)
            .border(1.dp, RiftTheme.colors.borderGrey),
        scrollbarModifier = Modifier.padding(vertical = Spacing.small),
    ) {
        for (channel in intelChannels) {
            key(channel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverBackground()
                        .padding(Spacing.small),
                ) {
                    val text = buildAnnotatedString {
                        append(channel.name)
                        withStyle(SpanStyle(color = RiftTheme.colors.textSecondary)) {
                            append(" – ${channel.region}")
                        }
                    }
                    Text(
                        text = text,
                        style = RiftTheme.typography.bodyPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    RiftImageButton(
                        resource = Res.drawable.deleteicon,
                        size = 20.dp,
                        onClick = { onIntelChannelDelete(channel) },
                    )
                }
            }
        }
        if (intelChannels.isEmpty()) {
            Text(
                text = "No intel channels configured",
                style = RiftTheme.typography.titlePrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.large)
                    .padding(horizontal = Spacing.large),
            )
            if (suggestedIntelChannels != null) {
                Text(
                    text = suggestedIntelChannels.promptTitleText,
                    style = RiftTheme.typography.bodyPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.medium)
                        .padding(horizontal = Spacing.large),
                )
                RiftButton(
                    text = suggestedIntelChannels.promptButtonText,
                    type = ButtonType.Primary,
                    cornerCut = ButtonCornerCut.Both,
                    onClick = onSuggestedIntelChannelsClick,
                    modifier = Modifier
                        .padding(top = Spacing.medium)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(top = Spacing.medium),
    ) {
        var addChannelText by remember { mutableStateOf("") }
        RiftTextField(
            text = addChannelText,
            placeholder = "Channel name",
            onTextChanged = {
                addChannelText = it
            },
            modifier = Modifier.weight(1f),
        )
        val regionPlaceholder = "Choose region"
        var selectedRegion by remember { mutableStateOf(regionPlaceholder) }
        RiftDropdown(
            items = regions,
            selectedItem = selectedRegion,
            onItemSelected = { selectedRegion = it },
            getItemName = { it },
            maxItems = 5,
        )
        RiftButton("Add channel", onClick = {
            if (addChannelText.isNotEmpty() && selectedRegion != regionPlaceholder) {
                onIntelChannelAdded(addChannelText, selectedRegion)
                addChannelText = ""
                selectedRegion = regionPlaceholder
            }
        })
    }
}

@Composable
private fun IntelSection(
    isShowingSystemDistance: Boolean,
    onIsShowingSystemDistanceChange: (Boolean) -> Unit,
    isUsingJumpBridgesForDistance: Boolean,
    onIsUsingJumpBridgesForDistance: (Boolean) -> Unit,
    intelExpireSeconds: Int,
    onIntelExpireSecondsChange: (Int) -> Unit,
) {
    SectionTitle("Intel", Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = "Show distance on systems",
        tooltip = "Enable to show the number of jumps to\nthe closest character next to system names.\nOnly shows for up to 9 jumps away.",
        isChecked = isShowingSystemDistance,
        onCheckedChange = onIsShowingSystemDistanceChange,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Use jump bridges for distance",
        tooltip = "Enable to include jump bridges in system distances",
        isChecked = isUsingJumpBridgesForDistance,
        onCheckedChange = onIsUsingJumpBridgesForDistance,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    val expiryItems = mapOf(
        "1 minute" to 60,
        "2 minutes" to 60 * 2,
        "5 minutes" to 60 * 5,
        "15 minutes" to 60 * 15,
        "30 minutes" to 60 * 30,
        "1 hour" to 60 * 60,
        "Don't expire" to Int.MAX_VALUE,
    )
    RiftDropdownWithLabel(
        label = "Expire intel after:",
        items = expiryItems.values.toList(),
        selectedItem = intelExpireSeconds,
        onItemSelected = onIntelExpireSecondsChange,
        getItemName = { item -> expiryItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
        tooltip = """
                    Time after a piece of intel will no longer
                    be shown on the feed or map.
        """.trimIndent(),
    )
}
