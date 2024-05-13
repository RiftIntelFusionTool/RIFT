package dev.nohus.rift.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ClickablePlayer
import dev.nohus.rift.compose.ClickableSystem
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SystemEntities
import dev.nohus.rift.compose.modifyIfNotNull
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.generated.resources.window_titlebar_close
import dev.nohus.rift.notifications.NotificationsController.Notification
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.utils.Pos
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NotificationWindow(
    notifications: List<Notification>,
    position: Pos?,
    onHoldDisappearance: (hold: Boolean) -> Unit,
    onCloseClick: (Notification) -> Unit,
) {
    val state = rememberWindowState(
        width = Dp.Unspecified,
        height = Dp.Unspecified,
        position = position?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.PlatformDefault,
    )
    Window(
        onCloseRequest = {},
        state = state,
        undecorated = true,
        alwaysOnTop = true,
        resizable = false,
        focusable = false,
        title = "Notification",
        icon = painterResource(Res.drawable.window_loudspeaker_icon),
    ) {
        var boxSize by remember { mutableStateOf<IntSize?>(null) }
        val density = LocalDensity.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(Color.Black)
                .border(1.dp, RiftTheme.colors.borderGreyLight)
                .pointerHoverIcon(PointerIcon(Cursors.pointer))
                .onSizeChanged {
                    // Force first measured size to be kept. On macOS for some reason there is a second measure
                    // that is 1 pixel smaller, making text wrap, this will make it ignore it
                    if (boxSize == null) boxSize = it
                }
                .modifyIfNotNull(boxSize) {
                    with(density) {
                        requiredSize(it.width.toDp(), it.height.toDp())
                    }
                }
                .onPointerEvent(PointerEventType.Enter) {
                    onHoldDisappearance(true)
                }
                .onPointerEvent(PointerEventType.Exit) {
                    onHoldDisappearance(false)
                },
        ) {
            ScrollbarColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(vertical = Spacing.medium),
                scrollbarModifier = Modifier.padding(vertical = Spacing.small),
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .width(IntrinsicSize.Max),
            ) {
                notifications.reversed().forEachIndexed { index, notification ->
                    if (index != 0) Divider(color = RiftTheme.colors.borderGreyLight, thickness = 1.dp)
                    NotificationContent(
                        notification = notification,
                        onCloseClick = { onCloseClick(notification) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationContent(
    notification: Notification,
    onCloseClick: () -> Unit,
) {
    when (notification) {
        is Notification.TextNotification -> {
            NotificationTitle(
                title = buildAnnotatedString {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                        append(notification.title)
                    }
                },
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                if (notification.characterId != null) {
                    Character(notification.characterId, "Your character")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (notification.typeId != null) {
                        AsyncTypeIcon(
                            typeId = notification.typeId,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            val annotations = notification.message
                                .getStringAnnotations(
                                    Notification.TextNotification.styleTag,
                                    0,
                                    notification.message.length,
                                )
                            val styledMessage = buildAnnotatedString {
                                append(notification.message)
                                annotations.forEach { annotation ->
                                    addStyle(
                                        SpanStyle(color = RiftTheme.colors.textHighlighted),
                                        annotation.start,
                                        annotation.end,
                                    )
                                }
                            }
                            append(styledMessage)
                        },
                        style = RiftTheme.typography.titlePrimary,
                    )
                }
            }
        }

        is Notification.ChatMessageNotification -> {
            NotificationTitle(
                title = buildAnnotatedString {
                    append("Chat message in ")
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                        append(notification.channel)
                    }
                },
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (notification.senderCharacterId != null) {
                        ClickablePlayer(notification.senderCharacterId) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncPlayerPortrait(
                                    characterId = notification.senderCharacterId,
                                    size = 32,
                                    modifier = Modifier.size(32.dp),
                                )
                                Text(
                                    text = notification.sender,
                                    style = RiftTheme.typography.titlePrimary,
                                )
                            }
                        }
                    }
                    Text(
                        text = buildAnnotatedString {
                            if (notification.senderCharacterId == null) {
                                append(notification.sender)
                            }
                            append(" > ")
                            append(notification.message)
                        },
                        style = RiftTheme.typography.titlePrimary,
                    )
                }
            }
        }

        is Notification.JabberMessageNotification -> {
            NotificationTitle(
                title = buildAnnotatedString {
                    append("Jabber message ")
                    if (notification.sender == notification.chat) {
                        append("from ")
                    } else {
                        append("in ")
                    }
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                        append(notification.chat)
                    }
                },
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            if (notification.sender != notification.chat) {
                                append(notification.sender)
                                append(" > ")
                            }
                            append(notification.message)
                        },
                        style = RiftTheme.typography.titlePrimary,
                    )
                }
            }
        }

        is Notification.IntelNotification -> {
            NotificationTitle(
                title = AnnotatedString(notification.title),
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                when (notification.locationMatch) {
                    is AlertsTriggerController.AlertLocationMatch.System -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val systemSubtext = when (val distance = notification.locationMatch.distance) {
                                0 -> "In the system"
                                1 -> "1 jump away"
                                else -> "$distance jumps away"
                            }
                            SolarSystem(notification.solarSystem, systemSubtext)
                            SolarSystem(notification.locationMatch.systemId, "Reference system")
                        }
                    }

                    is AlertsTriggerController.AlertLocationMatch.Character -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val systemSubtext = when (val distance = notification.locationMatch.distance) {
                                0 -> "In your system"
                                1 -> "1 jump away"
                                else -> "$distance jumps away"
                            }
                            SolarSystem(notification.solarSystem, systemSubtext)
                            Character(notification.locationMatch.characterId, "Your character")
                        }
                    }
                }
                Divider(color = RiftTheme.colors.borderGrey, modifier = Modifier.padding(start = Spacing.medium))
                SystemEntities(
                    entities = notification.systemEntities,
                    system = notification.solarSystem,
                )
            }
        }
    }
}

@Composable
private fun NotificationTitle(
    title: AnnotatedString,
    onCloseClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = RiftTheme.typography.titlePrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
                .padding(horizontal = Spacing.medium),
        )
        RiftImageButton(Res.drawable.window_titlebar_close, 16.dp, onCloseClick)
    }
}

@Composable
private fun SolarSystem(systemId: Int, subtext: String?) {
    val repository: SolarSystemsRepository by koin.inject()
    val systemName = repository.getSystemName(systemId) ?: return
    SolarSystem(systemName, subtext)
}

@Composable
private fun SolarSystem(system: String, subtext: String?) {
    val repository: SolarSystemsRepository by koin.inject()
    ClickableSystem(system) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            val sunTypeId = repository.getSystemSunTypeId(system)
            AsyncTypeIcon(
                typeId = sunTypeId,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(
                    text = system,
                    style = RiftTheme.typography.bodyLink,
                )
                if (subtext != null) {
                    Text(
                        text = subtext,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Character(characterId: Int, subtext: String) {
    val localCharactersRepository: LocalCharactersRepository by koin.inject()
    val name = localCharactersRepository.characters.value.firstOrNull { it.characterId == characterId }?.info?.success?.name ?: "Character"
    ClickablePlayer(characterId) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            AsyncPlayerPortrait(
                characterId = characterId,
                size = 32,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(
                    text = name,
                    style = RiftTheme.typography.bodyHighlighted,
                )
                Text(
                    text = subtext,
                    style = RiftTheme.typography.bodyPrimary,
                )
            }
        }
    }
}
