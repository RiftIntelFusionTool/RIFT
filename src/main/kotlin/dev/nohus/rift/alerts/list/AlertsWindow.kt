package dev.nohus.rift.alerts.list

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.alerts.AlertAction
import dev.nohus.rift.alerts.AlertTrigger
import dev.nohus.rift.alerts.ChatMessageChannel
import dev.nohus.rift.alerts.GameActionType
import dev.nohus.rift.alerts.IntelChannel
import dev.nohus.rift.alerts.IntelReportLocation
import dev.nohus.rift.alerts.IntelReportType
import dev.nohus.rift.alerts.JabberMessageChannel
import dev.nohus.rift.alerts.JabberPingType
import dev.nohus.rift.alerts.JumpRange
import dev.nohus.rift.alerts.PapType
import dev.nohus.rift.alerts.PiEventType
import dev.nohus.rift.alerts.create.CreateAlertDialog
import dev.nohus.rift.alerts.creategroup.CreateGroupDialog
import dev.nohus.rift.alerts.list.AlertsViewModel.UiState
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckbox
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.delete
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.toggle_off_18
import dev.nohus.rift.generated.resources.toggle_on_18
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.sound.Sound
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.nameWithoutExtension

@Composable
fun AlertsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: AlertsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Alerts",
        icon = Res.drawable.window_loudspeaker_icon,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        AlertsWindowContent(
            state = state,
            onAlertClick = viewModel::onAlertClick,
            onToggleAlert = viewModel::onToggleAlert,
            onGroupChange = viewModel::onGroupChange,
            onTestAlertSound = viewModel::onTestAlertSound,
            onEditAlertAction = viewModel::onEditAlertAction,
            onDeleteAlert = viewModel::onDeleteAlert,
            onCreateAlertClick = viewModel::onCreateAlertClick,
            onCreateGroupClick = viewModel::onCreateGroupClick,
            onGroupRenameClick = viewModel::onGroupRenameClick,
            onGroupDeleteClick = viewModel::onGroupDeleteClick,
            onGroupToggleAlerts = viewModel::onGroupToggleAlerts,
        )

        val isCreateAlertDialogOpen = state.isCreateAlertDialogOpen
        if (isCreateAlertDialogOpen != null) {
            CreateAlertDialog(
                inputModel = isCreateAlertDialogOpen,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseCreateAlert,
            )
        }
        val isCreateGroupDialogOpen = state.isCreateGroupDialogOpen
        if (isCreateGroupDialogOpen != null) {
            CreateGroupDialog(
                inputModel = isCreateGroupDialogOpen,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseCreateGroup,
                onConfirmClick = viewModel::onCreateGroupConfirm,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlertsWindowContent(
    state: UiState,
    onAlertClick: (id: String) -> Unit,
    onToggleAlert: (id: String, isEnabled: Boolean) -> Unit,
    onGroupChange: (id: String, group: String?) -> Unit,
    onTestAlertSound: (id: String) -> Unit,
    onEditAlertAction: (id: String) -> Unit,
    onDeleteAlert: (id: String) -> Unit,
    onCreateAlertClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupRenameClick: (group: String) -> Unit,
    onGroupDeleteClick: (group: String) -> Unit,
    onGroupToggleAlerts: (group: String?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (state.alerts.isNotEmpty()) {
            ScrollbarLazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                val nonEmptyGroups = state.alerts.mapNotNull { it.group }.toSet()
                val emptyGroups = state.groups - nonEmptyGroups
                state.alerts
                    .groupBy { it.group }
                    .let { it + emptyGroups.associateWith { emptyList() } }
                    .entries
                    .sortedWith(compareBy({ it.key == null }, { it.key }))
                    .forEach { (group, alertsInGroup) ->
                        stickyHeader {
                            AlertGroupHeader(
                                name = group,
                                isEmpty = alertsInGroup.isEmpty(),
                                hasEnabledAlerts = alertsInGroup.any { it.isEnabled },
                                onGroupToggleAlerts = { onGroupToggleAlerts(group) },
                                onGroupRenameClick = { onGroupRenameClick(group!!) },
                                onGroupDeleteClick = { onGroupDeleteClick(group!!) },
                            )
                        }
                        if (group in emptyGroups) {
                            item {
                                EmptyGroup()
                            }
                        }
                        items(alertsInGroup, key = { it.id }) { alert ->
                            val isExpanded = alert.id == state.expandedAlert
                            AlertItem(
                                onAlertClick = onAlertClick,
                                alert = alert,
                                onToggleAlert = onToggleAlert,
                                state = state,
                                isExpanded = isExpanded,
                                groups = state.groups,
                                onGroupChange = { onGroupChange(alert.id, it) },
                                onTestAlertSound = onTestAlertSound,
                                onEditAlertAction = onEditAlertAction,
                                onDeleteAlert = onDeleteAlert,
                            )
                        }
                    }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "No alerts defined.\nCreate some with the button below.",
                    style = RiftTheme.typography.titlePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.align(Alignment.End),
        ) {
            if (state.alerts.isNotEmpty()) {
                RiftButton(
                    text = "Create group",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onCreateGroupClick,
                )
            }
            RiftButton(
                text = "Create alert",
                onClick = onCreateAlertClick,
            )
        }
    }
}

@Composable
private fun LazyItemScope.AlertGroupHeader(
    name: String?,
    isEmpty: Boolean,
    hasEnabledAlerts: Boolean,
    onGroupToggleAlerts: () -> Unit,
    onGroupRenameClick: () -> Unit,
    onGroupDeleteClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .pointerInteraction(pointerState)
            .background(RiftTheme.colors.backgroundPrimary)
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize(),
    ) {
        Text(
            text = name ?: "Default",
            style = RiftTheme.typography.titlePrimary,
        )
        Spacer(Modifier.weight(1f))

        val buttonsAlpha by animateFloatAsState(if (pointerState.isHovered) 1f else 0f)
        if (!isEmpty) {
            RiftTooltipArea(
                text = if (hasEnabledAlerts) "Disable all alerts" else "Enable all alerts",
            ) {
                RiftImageButton(
                    resource = if (hasEnabledAlerts) Res.drawable.toggle_on_18 else Res.drawable.toggle_off_18,
                    size = 18.dp,
                    onClick = onGroupToggleAlerts,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
        if (name != null) { // Don't show actions for the default group
            RiftTooltipArea(
                text = "Rename group",
            ) {
                RiftImageButton(
                    resource = Res.drawable.editplanicon,
                    size = 20.dp,
                    onClick = onGroupRenameClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
            RiftTooltipArea(
                text = if (isEmpty) "Delete group" else "Delete group and move alerts to default",
            ) {
                RiftImageButton(
                    resource = Res.drawable.delete,
                    size = 20.dp,
                    onClick = onGroupDeleteClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.EmptyGroup() {
    Row(
        modifier = Modifier
            .padding(vertical = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize(),
    ) {
        Text(
            text = "No alerts in this group",
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.AlertItem(
    onAlertClick: (id: String) -> Unit,
    alert: Alert,
    onToggleAlert: (id: String, isEnabled: Boolean) -> Unit,
    state: UiState,
    isExpanded: Boolean,
    groups: Set<String>,
    onGroupChange: (group: String?) -> Unit,
    onTestAlertSound: (id: String) -> Unit,
    onEditAlertAction: (id: String) -> Unit,
    onDeleteAlert: (id: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .hoverBackground()
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .padding(vertical = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize()
            .onClick { onAlertClick(alert.id) },
    ) {
        val alpha = if (alert.isEnabled) 1f else 0.5f
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.small),
        ) {
            RiftCheckbox(
                isChecked = alert.isEnabled,
                onCheckedChange = { onToggleAlert(alert.id, it) },
            )
            val text = getAlertText(alert, state.characters, state.sounds)

            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .alpha(alpha)
                    .padding(horizontal = Spacing.medium),
            )
        }
        if (isExpanded) {
            listOfNotNull(
                getSpecificCharactersDetailText(alert),
                getSpecificShipClassesDetailText(alert),
                getSpecificFleetCommandersDetailText(alert),
                getDecloakIgnoredKeywordsDetailText(alert),
                getSpecificColoniesDetailText(alert, state.colonies),
            ).forEach {
                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.small)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = it,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(alpha),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(horizontal = Spacing.medium)
                    .fillMaxWidth(),
            ) {
                RiftDropdownWithLabel(
                    label = "Group:",
                    items = (groups.sorted() + listOf(null)).toList(),
                    selectedItem = alert.group,
                    onItemSelected = onGroupChange,
                    getItemName = { it ?: "Default" },
                    maxItems = 3,
                    modifier = Modifier
                        .widthIn(max = 170.dp)
                        .padding(end = Spacing.medium),
                )
                if (alert.actions.any { it is AlertAction.Sound || it is AlertAction.CustomSound }) {
                    RiftButton(
                        text = "Test sound",
                        type = ButtonType.Secondary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = { onTestAlertSound(alert.id) },
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                }
                RiftButton(
                    text = "Edit action",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = { onEditAlertAction(alert.id) },
                    modifier = Modifier.padding(end = Spacing.medium),
                )
                RiftButton(
                    text = "Delete",
                    type = ButtonType.Negative,
                    onClick = { onDeleteAlert(alert.id) },
                )
            }
        }
    }
}

@Composable
private fun getSpecificCharactersDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val specificCharacters = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.SpecificCharacters }
        if (specificCharacters != null) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            val characters = (specificCharacters as IntelReportType.SpecificCharacters).characters
            buildAnnotatedString {
                withStyle(secondary) {
                    append("Monitored characters: ")
                    characters.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != characters.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getSpecificFleetCommandersDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.JabberPing && alert.trigger.pingType is JabberPingType.Fleet) {
        val fleetCommanders = alert.trigger.pingType.fleetCommanders
        if (fleetCommanders.isNotEmpty()) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            buildAnnotatedString {
                withStyle(secondary) {
                    append("Fleet commanders: ")
                    fleetCommanders.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != fleetCommanders.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getDecloakIgnoredKeywordsDetailText(alert: Alert): AnnotatedString? {
    val decloakedTrigger = (alert.trigger as? AlertTrigger.GameAction)?.actionTypes
        ?.filterIsInstance<GameActionType.Decloaked>()?.firstOrNull() ?: return null
    return if (decloakedTrigger.ignoredKeywords.isNotEmpty()) {
        val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
        val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
        buildAnnotatedString {
            withStyle(secondary) {
                append("Ignore decloaking objects containing: ")
                decloakedTrigger.ignoredKeywords.forEach { keyword ->
                    withStyle(primary) {
                        append(keyword)
                    }
                    if (keyword != decloakedTrigger.ignoredKeywords.last()) {
                        append(", ")
                    }
                }
            }
        }
    } else {
        null
    }
}

@Composable
private fun getSpecificColoniesDetailText(alert: Alert, colonies: List<ColonyItem>): AnnotatedString? {
    val colonyIds = (alert.trigger as? AlertTrigger.PlanetaryIndustry)?.coloniesFilter ?: return null
    val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
    val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
    return buildAnnotatedString {
        withStyle(secondary) {
            append("Target colonies:")
            colonyIds
                .mapNotNull { id ->
                    val colony = colonies.find { it.colony.id == id } ?: return@mapNotNull null
                    colony.colony.characterId to colony
                }.groupBy {
                    it.first
                }.forEach { (_, entries) ->
                    val items = entries.map { it.second }
                    val characterName = items.first().characterName ?: "Loading…"
                    append("\n")
                    withStyle(primary) {
                        append(characterName)
                    }
                    append(": ")
                    items.forEachIndexed { index, colony ->
                        if (index != 0) append(", ")
                        withStyle(primary) {
                            append(colony.colony.planet.name)
                        }
                    }
                }
        }
    }
}

@Composable
private fun getSpecificShipClassesDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val specificClasses = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.SpecificShipClasses }
        if (specificClasses != null) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            val characters = (specificClasses as IntelReportType.SpecificShipClasses).classes
            buildAnnotatedString {
                withStyle(secondary) {
                    append("Monitored ship classes: ")
                    characters.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != characters.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getAlertText(
    alert: Alert,
    characters: List<LocalCharacter>,
    sounds: List<Sound>,
): AnnotatedString {
    val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
    val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
    return buildAnnotatedString {
        withStyle(secondary) {
            append("When ")
            when (val trigger = alert.trigger) {
                is AlertTrigger.IntelReported -> {
                    if (trigger.reportTypes.size != 1) {
                        append("any of ")
                    }
                    val types = trigger.reportTypes.joinToString { type ->
                        when (type) {
                            IntelReportType.AnyCharacter -> "characters"
                            is IntelReportType.SpecificCharacters -> {
                                if (type.characters.size == 1) {
                                    type.characters.single()
                                } else {
                                    "${type.characters.size} specific characters"
                                }
                            }
                            IntelReportType.AnyShip -> "ships"
                            is IntelReportType.SpecificShipClasses -> {
                                if (type.classes.size == 1) {
                                    "${type.classes.single()}-class ships"
                                } else {
                                    "${type.classes.size} ship classes"
                                }
                            }
                            IntelReportType.Bubbles -> "bubbles"
                            IntelReportType.GateCamp -> "gate camps"
                            IntelReportType.Wormhole -> "wormholes"
                        }
                    }
                    withStyle(primary) {
                        append(types)
                    }
                    append(" are reported ")
                    val location = when (val location = trigger.reportLocation) {
                        is IntelReportLocation.System -> "${getRangePrefixText(location.jumpsRange)} ${location.systemName}"
                        is IntelReportLocation.AnyOwnedCharacter -> "${getRangePrefixText(location.jumpsRange)} any online character's location"
                        is IntelReportLocation.OwnedCharacter -> {
                            val character = characters.firstOrNull { it.characterId == location.characterId }?.info?.success?.name ?: location.characterId.toString()
                            "${getRangePrefixText(location.jumpsRange)} $character's location"
                        }
                    }
                    withStyle(primary) {
                        append(location)
                    }
                }
                is AlertTrigger.GameAction -> {
                    trigger.actionTypes.forEachIndexed { index, type ->
                        if (index != 0) append(", or ")
                        when (type) {
                            is GameActionType.InCombat -> {
                                append("you are ")
                                withStyle(primary) { append("in combat") }
                                if (type.nameContaining != null) {
                                    append(" with ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                            }
                            is GameActionType.UnderAttack -> {
                                append("you are ")
                                withStyle(primary) { append("under attack") }
                                if (type.nameContaining != null) {
                                    append(" by ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                            }
                            is GameActionType.Attacking -> {
                                append("you are ")
                                withStyle(primary) { append("attacking") }
                                if (type.nameContaining != null) {
                                    append(" target ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                            }
                            GameActionType.BeingWarpScrambled -> {
                                append("you are ")
                                withStyle(primary) { append("being warp scrambled") }
                            }
                            is GameActionType.Decloaked -> {
                                append("you are ")
                                withStyle(primary) { append("decloaked") }
                                if (type.ignoredKeywords.isNotEmpty()) {
                                    append(" with exceptions")
                                }
                            }
                            is GameActionType.CombatStopped -> {
                                append("you are ")
                                withStyle(primary) { append("no longer in combat") }
                                if (type.nameContaining != null) {
                                    append(" with ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                                append(" for ")
                                val minutes = type.durationSeconds / 60
                                withStyle(primary) {
                                    if (minutes == 1) {
                                        append("$minutes minute")
                                    } else if (minutes > 1) {
                                        append("$minutes minutes")
                                    } else {
                                        append("${type.durationSeconds} seconds")
                                    }
                                }
                            }
                        }
                    }
                }
                is AlertTrigger.PlanetaryIndustry -> {
                    append("on ")
                    val coloniesFilter = when {
                        trigger.coloniesFilter == null -> "any colony"
                        trigger.coloniesFilter.size == 1 -> "a specific colony"
                        else -> "${trigger.coloniesFilter.size} specific colonies"
                    }
                    withStyle(primary) {
                        append(coloniesFilter)
                    }
                    append(" ")
                    trigger.eventTypes.forEachIndexed { index, type ->
                        if (index != 0) append(", or ")
                        val text = when (type) {
                            PiEventType.ExtractorInactive -> "extractors stop"
                            PiEventType.Idle -> "production stops"
                            PiEventType.NotSetup -> "setup is unfinished"
                            PiEventType.StorageFull -> "storage becomes full"
                        }
                        withStyle(primary) {
                            append(text)
                        }
                    }
                    if (trigger.alertBeforeSeconds > 0) {
                        val duration = Duration.ofSeconds(trigger.alertBeforeSeconds.toLong())
                        val text = when {
                            duration.toHours() >= 1 -> "${duration.toHours()} hour${duration.toHours().plural}"
                            else -> "${duration.toMinutes()} minutes"
                        }
                        append(" in ")
                        withStyle(primary) {
                            append(text)
                        }
                    }
                }
                is AlertTrigger.ChatMessage -> {
                    append("a chat message")
                    if (trigger.messageContaining != null) {
                        append(" containing ")
                        withStyle(primary) {
                            append(trigger.messageContaining)
                        }
                    }
                    append(" is sent")
                    if (trigger.sender != null) {
                        append(" by ")
                        withStyle(primary) {
                            append(trigger.sender)
                        }
                    }
                    append(" in ")
                    val channel = when (val channel = trigger.channel) {
                        ChatMessageChannel.Any -> "any channel"
                        is ChatMessageChannel.Channel -> channel.name
                    }
                    withStyle(primary) {
                        append(channel)
                    }
                }
                is AlertTrigger.JabberPing -> {
                    when (trigger.pingType) {
                        JabberPingType.Message -> {}
                        is JabberPingType.Message2 -> {
                            append("a message ping ")
                            if (trigger.pingType.target != null) {
                                append("for ")
                                withStyle(primary) {
                                    append(trigger.pingType.target)
                                }
                                append(" ")
                            }
                            append("is received")
                        }
                        is JabberPingType.Fleet -> {
                            append("a fleet ping ")
                            if (trigger.pingType.target != null) {
                                append("for ")
                                withStyle(primary) {
                                    append(trigger.pingType.target)
                                }
                                append(" ")
                            }
                            append("is received")
                            if (trigger.pingType.fleetCommanders.isNotEmpty()) {
                                append(", with ")
                                if (trigger.pingType.fleetCommanders.size == 1) {
                                    withStyle(primary) {
                                        append(trigger.pingType.fleetCommanders.single())
                                        append(" as FC")
                                    }
                                } else {
                                    withStyle(primary) {
                                        append("${trigger.pingType.fleetCommanders.size} specific FC's")
                                    }
                                }
                            }
                            if (trigger.pingType.formupSystem != null) {
                                append(", forming in ")
                                withStyle(primary) {
                                    append(trigger.pingType.formupSystem)
                                }
                            }
                            if (trigger.pingType.papType != PapType.Any) {
                                append(", with ")
                                withStyle(primary) {
                                    val type = when (trigger.pingType.papType) {
                                        PapType.Any -> "any"
                                        PapType.Peacetime -> "Peacetime"
                                        PapType.Strategic -> "Strategic"
                                    }
                                    append(type)
                                }
                                append(" PAP")
                            }
                            if (trigger.pingType.doctrineContaining != null) {
                                append(", with doctrine containing ")
                                withStyle(primary) {
                                    append(trigger.pingType.doctrineContaining)
                                }
                            }
                        }
                    }
                }
                is AlertTrigger.JabberMessage -> {
                    append("a Jabber message")
                    if (trigger.messageContaining != null) {
                        append(" containing ")
                        withStyle(primary) {
                            append(trigger.messageContaining)
                        }
                    }
                    append(" is sent")
                    if (trigger.sender != null) {
                        append(" by ")
                        withStyle(primary) {
                            append(trigger.sender)
                        }
                    }
                    append(" in ")
                    val channel = when (val channel = trigger.channel) {
                        JabberMessageChannel.Any -> "any chat"
                        is JabberMessageChannel.Channel -> channel.name
                        JabberMessageChannel.DirectMessage -> "a direct message"
                    }
                    withStyle(primary) {
                        append(channel)
                    }
                }
                is AlertTrigger.NoChannelActivity -> {
                    append("no message is received in ")
                    val channel = when (val channel = trigger.channel) {
                        IntelChannel.All -> "all intel channels"
                        IntelChannel.Any -> "any intel channel"
                        is IntelChannel.Channel -> channel.name
                    }
                    withStyle(primary) {
                        append(channel)
                    }
                    append(" for ")
                    val minutes = trigger.durationSeconds / 60
                    withStyle(primary) {
                        if (minutes == 1) {
                            append("$minutes minute")
                        } else if (minutes > 1) {
                            append("$minutes minutes")
                        } else {
                            append("${trigger.durationSeconds} seconds")
                        }
                    }
                }
            }
            append(" then ")
            val actions = alert.actions.joinToString { action ->
                when (action) {
                    AlertAction.RiftNotification -> "send a RIFT notification"
                    AlertAction.SystemNotification -> "send a system notification"
                    AlertAction.PushNotification -> "send a push notification"
                    is AlertAction.Sound -> "play sound \"${sounds.firstOrNull { it.id == action.id }?.name ?: "?"}\""
                    is AlertAction.CustomSound -> "play sound ${Path.of(action.path).nameWithoutExtension}"
                    AlertAction.ShowPing -> "show the ping"
                    AlertAction.ShowColonies -> "show colonies"
                }
            }
            withStyle(primary) {
                append(actions)
            }
            if (alert.cooldownSeconds != 0) {
                append(", don't repeat for ")
                val text = when (val minutes = alert.cooldownSeconds / 60) {
                    0 -> "${alert.cooldownSeconds} seconds"
                    1 -> "1 minute"
                    else -> "$minutes minutes"
                }
                withStyle(primary) {
                    append(text)
                }
            }
        }
    }
}

private fun getRangePrefixText(range: JumpRange): String {
    val (min, max) = range.min to range.max
    val plural = if (max > 1) "s" else ""
    return if (min == 0 && max == 0) {
        "in"
    } else if (min == 0) {
        "up to $max jump$plural from"
    } else if (min == max) {
        "exactly $max jump$plural from"
    } else {
        "between $min–$max jump$plural from"
    }
}
