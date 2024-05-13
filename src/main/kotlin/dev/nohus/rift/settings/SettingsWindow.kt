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
import dev.nohus.rift.compose.TooltipAnchor
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.notifications.NotificationEditWindow
import dev.nohus.rift.repositories.ConfigurationPackRepository.SuggestedIntelChannels
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
            intelChannels = state.intelChannels,
            suggestedIntelChannels = state.suggestedIntelChannels,
            regions = state.regions,
            logsDirectory = state.logsDirectory,
            isLogsDirectoryValid = state.isLogsDirectoryValid,
            settingsDirectory = state.settingsDirectory,
            isSettingsDirectoryValid = state.isSettingsDirectoryValid,
            isLoadOldMessagesEnabled = state.isLoadOldMessagesEnabled,
            isDisplayEveTime = state.isDisplayEveTime,
            isShowSetupWizardOnNextStartEnabled = state.isShowSetupWizardOnNextStartEnabled,
            isRememberOpenWindowsEnabled = state.isRememberOpenWindows,
            isRememberWindowPlacementEnabled = state.isRememberWindowPlacement,
            isUsingDarkTrayIcon = state.isUsingDarkTrayIcon,
            soundsVolume = state.soundsVolume,
            configurationPack = state.configurationPack,
            onSuggestedIntelChannelsClick = viewModel::onSuggestedIntelChannelsClick,
            onIntelChannelAdded = viewModel::onIntelChannelAdded,
            onIntelChannelDelete = viewModel::onIntelChannelDelete,
            onLogsDirectoryChanged = viewModel::onLogsDirectoryChanged,
            onDetectLogsDirectoryClick = viewModel::onDetectLogsDirectoryClick,
            onSettingsDirectoryChanged = viewModel::onSettingsDirectoryChanged,
            onDetectSettingsDirectoryClick = viewModel::onDetectSettingsDirectoryClick,
            onLoadOldMessagesChanged = viewModel::onLoadOldMessagedChanged,
            onShowSetupWizardOnNextStartChanged = viewModel::onShowSetupWizardOnNextStartChanged,
            onIsDisplayEveTimeChanged = viewModel::onIsDisplayEveTimeChanged,
            onRememberOpenWindowsChanged = viewModel::onRememberOpenWindowsChanged,
            onRememberWindowPlacementChanged = viewModel::onRememberWindowPlacementChanged,
            onEditNotificationClick = viewModel::onEditNotificationClick,
            onIsUsingDarkTrayIconChanged = viewModel::onIsUsingDarkTrayIconChanged,
            onSoundsVolumeChange = viewModel::onSoundsVolumeChange,
            onConfigurationPackChange = viewModel::onConfigurationPackChange,
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
    intelChannels: List<IntelChannel>,
    suggestedIntelChannels: SuggestedIntelChannels?,
    regions: List<String>,
    logsDirectory: String,
    isLogsDirectoryValid: Boolean,
    settingsDirectory: String,
    isSettingsDirectoryValid: Boolean,
    isLoadOldMessagesEnabled: Boolean,
    isDisplayEveTime: Boolean,
    isShowSetupWizardOnNextStartEnabled: Boolean,
    isRememberOpenWindowsEnabled: Boolean,
    isRememberWindowPlacementEnabled: Boolean,
    isUsingDarkTrayIcon: Boolean,
    onSuggestedIntelChannelsClick: () -> Unit,
    configurationPack: ConfigurationPack?,
    onIntelChannelAdded: (name: String, region: String) -> Unit,
    onIntelChannelDelete: (IntelChannel) -> Unit,
    onLogsDirectoryChanged: (String) -> Unit,
    onDetectLogsDirectoryClick: () -> Unit,
    onSettingsDirectoryChanged: (String) -> Unit,
    onDetectSettingsDirectoryClick: () -> Unit,
    onLoadOldMessagesChanged: (Boolean) -> Unit,
    onShowSetupWizardOnNextStartChanged: (Boolean) -> Unit,
    onIsDisplayEveTimeChanged: (Boolean) -> Unit,
    onRememberOpenWindowsChanged: (Boolean) -> Unit,
    onRememberWindowPlacementChanged: (Boolean) -> Unit,
    onEditNotificationClick: () -> Unit,
    onIsUsingDarkTrayIconChanged: (Boolean) -> Unit,
    soundsVolume: Int,
    onSoundsVolumeChange: (Int) -> Unit,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
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
                    intelChannels = intelChannels,
                    onIntelChannelDelete = onIntelChannelDelete,
                    regions = regions,
                    suggestedIntelChannels = suggestedIntelChannels,
                    onSuggestedIntelChannelsClick = onSuggestedIntelChannelsClick,
                    onIntelChannelAdded = onIntelChannelAdded,
                )
            }

            SectionContainer(inputModel, SettingsInputModel.EveInstallation) {
                EveInstallationSection(
                    isLogsDirectoryValid = isLogsDirectoryValid,
                    logsDirectory = logsDirectory,
                    onLogsDirectoryChanged = onLogsDirectoryChanged,
                    onDetectLogsDirectoryClick = onDetectLogsDirectoryClick,
                    isSettingsDirectoryValid = isSettingsDirectoryValid,
                    settingsDirectory = settingsDirectory,
                    onSettingsDirectoryChanged = onSettingsDirectoryChanged,
                    onDetectSettingsDirectoryClick = onDetectSettingsDirectoryClick,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.weight(1f),
        ) {
            SectionContainer(inputModel) {
                UserInterfaceSection(
                    isRememberOpenWindowsEnabled = isRememberOpenWindowsEnabled,
                    onRememberOpenWindowsChanged = onRememberOpenWindowsChanged,
                    isRememberWindowPlacementEnabled = isRememberWindowPlacementEnabled,
                    onRememberWindowPlacementChanged = onRememberWindowPlacementChanged,
                    isDisplayEveTime = isDisplayEveTime,
                    onIsDisplayEveTimeChanged = onIsDisplayEveTimeChanged,
                    isUsingDarkTrayIcon = isUsingDarkTrayIcon,
                    onIsUsingDarkTrayIconChanged = onIsUsingDarkTrayIconChanged,
                )
            }

            SectionContainer(inputModel) {
                AlertsSection(
                    soundsVolume = soundsVolume,
                    onSoundsVolumeChange = onSoundsVolumeChange,
                    onEditNotificationClick = onEditNotificationClick,
                )
            }

            SectionContainer(inputModel) {
                OtherSettingsSection(
                    configurationPack = configurationPack,
                    onConfigurationPackChange = onConfigurationPackChange,
                    isLoadOldMessagesEnabled = isLoadOldMessagesEnabled,
                    onLoadOldMessagesChanged = onLoadOldMessagesChanged,
                    isShowSetupWizardOnNextStartEnabled = isShowSetupWizardOnNextStartEnabled,
                    onShowSetupWizardOnNextStartChanged = onShowSetupWizardOnNextStartChanged,
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
        isTooltipBelow = true,
        isChecked = isRememberOpenWindowsEnabled,
        onCheckedChange = onRememberOpenWindowsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Remember window placement",
        tooltip = "Enable to remember window positions and\nsizes across app restarts",
        isTooltipBelow = true,
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
        getItemName = {
            when (it) {
                ConfigurationPack.Imperium -> "The Imperium"
                null -> "Default"
            }
        },
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
            tooltipAnchor = TooltipAnchor.BottomCenter,
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
            tooltipAnchor = TooltipAnchor.BottomCenter,
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
