package dev.nohus.rift.characters

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nohus.rift.characters.CharactersViewModel.CharacterItem
import dev.nohus.rift.characters.CharactersViewModel.CopyingState
import dev.nohus.rift.characters.CharactersViewModel.UiState
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ClickableLocation
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftIconButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.TooltipAnchor
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.recall_drones_16px
import dev.nohus.rift.generated.resources.sso
import dev.nohus.rift.generated.resources.sso_dark
import dev.nohus.rift.generated.resources.window_characters
import dev.nohus.rift.location.CharacterLocationRepository.Location
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.sso.SsoAuthority
import dev.nohus.rift.sso.SsoDialog
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.text.NumberFormat

@Composable
fun CharactersWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: CharactersViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Characters",
        icon = Res.drawable.window_characters,
        state = windowState,
        tuneContextMenuItems = listOf(
            ContextMenuItem.TextItem("Unhide characters", onClick = viewModel::onClearHiddenCharactersClick),
        ),
        onCloseClick = onCloseRequest,
    ) {
        CharactersWindowContent(
            state = state,
            onSsoClick = viewModel::onSsoClick,
            onCopySettingsClick = viewModel::onCopySettingsClick,
            onCopyCancel = viewModel::onCopyCancel,
            onCopySourceClick = viewModel::onCopySourceClick,
            onCopyDestinationClick = viewModel::onCopyDestinationClick,
            onCopySettingsConfirmClick = viewModel::onCopySettingsConfirmClick,
            onHideCharacterClick = viewModel::onHideCharacterClick,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }

        if (state.isSsoDialogOpen) {
            SsoDialog(
                inputModel = SsoAuthority.Eve,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseSso,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharactersWindowContent(
    state: UiState,
    onSsoClick: () -> Unit,
    onCopySettingsClick: () -> Unit,
    onCopyCancel: () -> Unit,
    onCopySourceClick: (Int) -> Unit,
    onCopyDestinationClick: (Int) -> Unit,
    onCopySettingsConfirmClick: () -> Unit,
    onHideCharacterClick: (characterId: Int) -> Unit,
) {
    if (state.characters.isNotEmpty()) {
        Column {
            TopRow(
                state = state,
                onSsoClick = onSsoClick,
                onCopySettingsClick = onCopySettingsClick,
                onCopyCancel = onCopyCancel,
                onCopySettingsConfirmClick = onCopySettingsConfirmClick,
            )

            ScrollbarLazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(state.characters, key = { it.characterId }) { character ->
                    CharacterRow(
                        character = character,
                        isOnline = character.characterId in state.onlineCharacters,
                        location = state.locations[character.characterId],
                        copyingState = state.copying,
                        onCopySourceClick = { onCopySourceClick(character.characterId) },
                        onCopyDestinationClick = { onCopyDestinationClick(character.characterId) },
                        onHideCharacterClick = onHideCharacterClick,
                        modifier = Modifier.animateItemPlacement(),
                    )
                }
            }
        }
    } else {
        Text(
            text = "No characters found.\n\nMake sure the game directory is selected in settings, and that you have logged in to at least one character on this computer before.",
            style = RiftTheme.typography.titlePrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.medium),
        )
    }
}

@Composable
private fun TopRow(
    state: UiState,
    onSsoClick: () -> Unit,
    onCopySettingsClick: () -> Unit,
    onCopyCancel: () -> Unit,
    onCopySettingsConfirmClick: () -> Unit,
) {
    AnimatedContent(state.copying, contentKey = { it::class }) { copying ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(bottom = Spacing.medium).fillMaxWidth(),
        ) {
            when (copying) {
                is CopyingState.NotCopying -> {
                    SsoButton(onClick = onSsoClick)
                    RiftTooltipArea(
                        tooltip = "Copy Eve settings\n(window positions, overview, etc.)\nbetween selected characters.",
                        anchor = TooltipAnchor.TopEnd,
                        contentAnchor = Alignment.BottomCenter,
                    ) {
                        RiftButton(
                            text = "Copy settings",
                            onClick = onCopySettingsClick,
                        )
                    }
                }

                is CopyingState.DestinationSelected -> {
                    AnimatedContent(copying, modifier = Modifier.weight(1f)) { copying ->
                        Text(
                            text = "Copy Eve settings from ${copying.source.name} to ${copying.destination.joinToString { it.name }}?",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                    RiftButton(
                        text = "Cancel",
                        type = ButtonType.Secondary,
                        onClick = onCopyCancel,
                    )
                    RiftButton(
                        text = "Confirm",
                        onClick = onCopySettingsConfirmClick,
                    )
                }

                is CopyingState.SelectingDestination -> {
                    Text(
                        text = "Select character to paste Eve settings to.",
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    RiftButton(
                        text = "Cancel",
                        type = ButtonType.Secondary,
                        onClick = onCopyCancel,
                    )
                }

                is CopyingState.SelectingSource -> {
                    Text(
                        text = "Select character to copy Eve settings from.",
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    RiftButton(
                        text = "Cancel",
                        type = ButtonType.Secondary,
                        onClick = onCopyCancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun SsoButton(
    onClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    Box(
        modifier = Modifier
            .pointerInteraction(pointerState)
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .padding(end = Spacing.small)
            .clickable { onClick() },
    ) {
        AnimatedContent(
            targetState = pointerState.isHovered,
            transitionSpec = {
                fadeIn(animationSpec = tween(300))
                    .togetherWith(fadeOut(animationSpec = tween(300)))
            },
        ) { isHovered ->
            val image = if (isHovered) Res.drawable.sso else Res.drawable.sso_dark
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.height(30.dp),
            )
        }
    }
}

@Composable
private fun CharacterRow(
    character: CharacterItem,
    isOnline: Boolean,
    location: Location?,
    copyingState: CopyingState,
    onCopySourceClick: () -> Unit,
    onCopyDestinationClick: () -> Unit,
    onHideCharacterClick: (characterId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    RiftContextMenuArea(
        items = listOf(
            ContextMenuItem.TextItem("Hide character", onClick = { onHideCharacterClick(character.characterId) }),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .hoverBackground()
                .padding(Spacing.verySmall),
        ) {
            OnlineIndicatorBar(isOnline)
            AsyncPlayerPortrait(
                characterId = character.characterId,
                size = 64,
                modifier = Modifier.size(64.dp),
            )
            when (character.info) {
                is AsyncResource.Ready -> {
                    Column(
                        modifier = Modifier.padding(start = Spacing.medium),
                    ) {
                        AsyncCorporationLogo(
                            corporationId = character.info.value.corporationId,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                        if (character.info.value.allianceId != null) {
                            AsyncAllianceLogo(
                                allianceId = character.info.value.allianceId,
                                size = 32,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = Spacing.medium)
                            .weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = character.info.value.name,
                                style = RiftTheme.typography.titleHighlighted,
                            )
                            OnlineIndicatorDot(
                                isOnline = isOnline,
                                modifier = Modifier.padding(horizontal = Spacing.medium),
                            )
                        }
                        Text(
                            text = character.info.value.corporationName,
                            style = RiftTheme.typography.bodySecondary,
                        )
                        if (character.info.value.allianceName != null) {
                            Text(
                                text = character.info.value.allianceName,
                                style = RiftTheme.typography.bodySecondary,
                            )
                        }
                        if (character.walletBalance != null) {
                            Text(
                                text = formatIsk(character.walletBalance),
                                style = RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }

                    AnimatedVisibility(copyingState is CopyingState.NotCopying) {
                        Location(location)
                    }

                    AnimatedContent(
                        copyingState,
                        contentKey = {
                            when (it) {
                                CopyingState.NotCopying -> 0
                                CopyingState.SelectingSource -> 1
                                is CopyingState.SelectingDestination -> 2
                                is CopyingState.DestinationSelected -> 2
                            }
                        },
                    ) { copyingState ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(32.dp),
                        ) {
                            when (copyingState) {
                                CopyingState.NotCopying -> {
                                    RequirementIcon(
                                        isFulfilled = character.isAuthenticated,
                                        fulfilledTooltip = "Authenticated with ESI",
                                        notFulfilledTooltip = "Not authenticated with ESI.\nClick the log in button above.",
                                        tooltipAnchor = TooltipAnchor.BottomEnd,
                                        modifier = Modifier.padding(start = Spacing.small),
                                    )
                                }

                                is CopyingState.SelectingSource -> {
                                    if (character.settingsFile != null) {
                                        RiftIconButton(
                                            icon = Res.drawable.copy_16px,
                                            onClick = onCopySourceClick,
                                        )
                                    }
                                }

                                is CopyingState.SelectingDestination -> {
                                    if (character.settingsFile != null) {
                                        if (copyingState.sourceId == character.characterId) {
                                            Image(
                                                painter = painterResource(Res.drawable.copy_16px),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(RiftTheme.colors.textPrimary),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        } else {
                                            RiftIconButton(
                                                icon = Res.drawable.recall_drones_16px,
                                                onClick = onCopyDestinationClick,
                                            )
                                        }
                                    }
                                }

                                is CopyingState.DestinationSelected -> {
                                    if (character.settingsFile != null) {
                                        if (copyingState.source.id == character.characterId) {
                                            Image(
                                                painter = painterResource(Res.drawable.copy_16px),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(RiftTheme.colors.textPrimary),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        } else if (copyingState.destination.any { it.id == character.characterId }) {
                                            Image(
                                                painter = painterResource(Res.drawable.recall_drones_16px),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(RiftTheme.colors.textPrimary),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        } else {
                                            RiftIconButton(
                                                icon = Res.drawable.recall_drones_16px,
                                                onClick = onCopyDestinationClick,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is AsyncResource.Error -> {
                    Text(
                        text = "Could not load",
                        style = RiftTheme.typography.bodySecondary.copy(color = RiftTheme.colors.borderError),
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )
                }

                AsyncResource.Loading -> {
                    Text(
                        text = "Loading…",
                        style = RiftTheme.typography.bodySecondary,
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )
                }
            }
        }
    }
}

private fun formatIsk(number: Double): String {
    val format = NumberFormat.getCompactNumberInstance()
    format.minimumFractionDigits = 1
    val formatted = format.format(number)
    return "$formatted ISK"
}

@Composable
private fun OnlineIndicatorBar(isOnline: Boolean) {
    val height by animateDpAsState(
        targetValue = if (isOnline) 64.dp else 0.dp,
        animationSpec = tween(1000),
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(height)
            .background(RiftTheme.colors.onlineGreen),
    ) {}
}

@Composable
fun OnlineIndicatorDot(
    isOnline: Boolean,
    modifier: Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (isOnline) RiftTheme.colors.onlineGreen else RiftTheme.colors.offlineRed,
        animationSpec = tween(1000),
    )
    val blur by animateFloatAsState(
        targetValue = if (isOnline) 4f else 1f,
        animationSpec = tween(1000),
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .border(2.dp, color, CircleShape),
        ) {}
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        ) {}
    }
}

@Composable
private fun Location(location: Location?) {
    if (location == null) return
    val repository: SolarSystemsRepository by koin.inject()
    val systemName = repository.getSystemName(location.solarSystemId) ?: return
    val locationId = location.station?.stationId?.toLong() ?: location.structure?.structureId

    ClickableLocation(location.solarSystemId, locationId) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val typeId = location.station?.typeId
                ?: location.structure?.typeId
                ?: repository.getSystemSunTypeId(systemName)
            val tooltip = location.station?.name?.replace(" - ", "\n")
                ?: location.structure?.name?.replace(" - ", "\n")
                ?: "In space"
            RiftTooltipArea(
                tooltip = tooltip,
                anchor = TooltipAnchor.BottomEnd,
            ) {
                AsyncTypeIcon(
                    typeId = typeId,
                    modifier = Modifier.size(32.dp).border(1.dp, RiftTheme.colors.borderGreyLight),
                )
            }
            Text(
                text = systemName,
                style = RiftTheme.typography.bodyLink,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(50.dp),
            )
        }
    }
}
