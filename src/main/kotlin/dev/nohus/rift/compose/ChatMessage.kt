package dev.nohus.rift.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.keywords_clear
import dev.nohus.rift.generated.resources.keywords_combat_probe
import dev.nohus.rift.generated.resources.keywords_ess
import dev.nohus.rift.generated.resources.keywords_gatecamp
import dev.nohus.rift.generated.resources.keywords_interdiction_probe
import dev.nohus.rift.generated.resources.keywords_killreport
import dev.nohus.rift.generated.resources.keywords_no_visual
import dev.nohus.rift.generated.resources.keywords_skyhook
import dev.nohus.rift.generated.resources.keywords_spike
import dev.nohus.rift.generated.resources.keywords_systems
import dev.nohus.rift.generated.resources.keywords_wormhole
import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.reports.settings.IntelReportsSettings
import dev.nohus.rift.logs.parse.ChatLogFileMetadata
import dev.nohus.rift.logs.parse.ChatMessage
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType
import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Link
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ChatMessage(
    settings: IntelReportsSettings,
    message: ParsedChannelChatMessage,
    alertTriggerTimestamp: Instant?,
    modifier: Modifier = Modifier,
) {
    val time = ZonedDateTime.ofInstant(message.chatMessage.timestamp, settings.displayTimezone).toLocalTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val formattedTime = formatter.format(time)
    val pointerState = remember { PointerInteractionStateHolder() }
    val background by pointerState.animateBackgroundHover()

    var animationProgress by remember { mutableStateOf(0f) }
    val animationCurve = (if (animationProgress < 0.5f) animationProgress else 1f - animationProgress) * 2
    if (alertTriggerTimestamp != null) {
        LaunchedEffect(alertTriggerTimestamp) {
            val animationDuration = 1_500
            val timeElapsed = Duration.between(alertTriggerTimestamp, Instant.now()).toMillis()
            animate(
                initialValue = 0f + (timeElapsed / animationDuration),
                targetValue = 1f,
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing),
            ) { value, _ -> animationProgress = value }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .pointerInteraction(pointerState)
            .background(background)
            .drawWithContent {
                drawContent()
                if (animationProgress > 0f) {
                    val width = 50
                    val x = -width + (width + size.width) * animationProgress
                    val color = Color.White.copy(alpha = animationCurve.coerceIn(0f..1f))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, color, Color.Transparent),
                            start = Offset(x, size.height * (1f - animationProgress)),
                            end = Offset(x + width, size.height * animationProgress),
                        ),
                    )
                }
            }
            .fillMaxWidth(),
    ) {
        Message(
            settings = settings,
            metadata = { MessageMetadata(settings, message, formattedTime, pointerState) },
            tokens = message.parsed,
        )
    }
}

@Composable
private fun MessageMetadata(
    settings: IntelReportsSettings,
    message: ParsedChannelChatMessage,
    formattedTime: String,
    pointerState: PointerInteractionStateHolder,
) {
    RiftTooltipArea(
        text = "Original message:\n${message.chatMessage.message}",
    ) {
        val background by pointerState.animateWindowBackgroundSecondaryHover()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .heightIn(min = settings.rowHeight)
                .border(1.dp, RiftTheme.colors.borderGrey)
                .background(background),
        ) {
            Text(
                text = formattedTime,
                modifier = Modifier.padding(4.dp),
            )
            if (settings.isShowingChannel) {
                VerticalDivider(color = RiftTheme.colors.borderGrey)
                Text(
                    text = message.metadata.channelName,
                    style = RiftTheme.typography.bodyHighlighted,
                    modifier = Modifier.padding(4.dp),
                )
            }
            if (settings.isShowingRegion) {
                VerticalDivider(color = RiftTheme.colors.borderGrey)
                Text(
                    text = message.channelRegion,
                    style = RiftTheme.typography.bodyHighlighted,
                    modifier = Modifier.padding(4.dp),
                )
            }
            if (settings.isShowingReporter) {
                VerticalDivider(color = RiftTheme.colors.borderGrey)
                Text(
                    text = message.chatMessage.author,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Message(
    settings: IntelReportsSettings,
    metadata: @Composable () -> Unit,
    tokens: List<Token>,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        metadata()
        for (token in tokens) {
            val types = token.types.filter { it !is Link }
            val text = token.words.joinToString(" ")
            if (types.size == 1) {
                when (val type = types.single()) {
                    is TokenType.Count -> TokenWithCount(settings.rowHeight, text)
                    is TokenType.Keyword -> TokenWithKeyword(settings.rowHeight, type.type)
                    is TokenType.Kill -> TokenWithKill(settings.rowHeight, type)
                    Link -> TokenWithText(settings.rowHeight, text, "Link")
                    is TokenType.Player -> TokenWithPlayer(settings.rowHeight, text, type.characterId)
                    is TokenType.Question -> TokenWithText(settings.rowHeight, text, "Question")
                    is TokenType.Ship -> TokenWithShip(settings.rowHeight, type)
                    is TokenType.System -> TokenWithSystem(settings.rowHeight, settings.isShowingSystemDistance, settings.isUsingJumpBridgesForDistance, type.name)
                    TokenType.Url -> TokenWithUrl(settings.rowHeight, text)
                    is TokenType.Gate -> {
                        val fromSystem = tokens.mapNotNull { it.types.filterIsInstance<TokenType.System>().firstOrNull() }.singleOrNull()?.name
                        TokenWithGate(settings.rowHeight, fromSystem, type.system, type.isAnsiblex)
                    }
                    is TokenType.Movement -> TokenWithMovement(settings.rowHeight, tokens, type)
                }
            } else if (types.size > 1) {
                TokenWithText(settings.rowHeight, text, "Multi")
            } else {
                TokenWithPlainText(settings.rowHeight, text)
            }
        }
    }
}

@Composable
private fun TokenWithText(rowHeight: Dp, text: String, type: String) {
    BorderedToken(rowHeight) {
        Text(
            text = type,
            modifier = Modifier.padding(4.dp),
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight)
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithCount(rowHeight: Dp, text: String) {
    BorderedToken(rowHeight) {
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithPlainText(rowHeight: Dp, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = rowHeight),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TokenWithUrl(rowHeight: Dp, text: String) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = rowHeight)
            .pointerHoverIcon(PointerIcon(Cursors.hand))
            .pointerInteraction(pointerInteractionStateHolder)
            .onClick { text.toURIOrNull()?.openBrowser() },
    ) {
        val underline = if (pointerInteractionStateHolder.current != PointerInteractionState.Normal) TextDecoration.Underline else TextDecoration.None
        Text(
            text = text,
            textDecoration = underline,
            style = RiftTheme.typography.bodyLink,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithShip(rowHeight: Dp, ship: TokenType.Ship) {
    val repository: ShipTypesRepository by koin.inject()
    val shipTypeId = repository.getShipTypeId(ship.name)
    ClickableShip(ship.name, shipTypeId) {
        BorderedToken(rowHeight) {
            AsyncTypeIcon(
                typeId = shipTypeId,
                modifier = Modifier.size(rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            val text = if (ship.count > 1) {
                "${ship.count}x ${ship.name}"
            } else if (ship.isPlural) {
                "${ship.name}s"
            } else {
                ship.name
            }
            Text(
                text = text,
                style = RiftTheme.typography.bodyHighlighted,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun TokenWithSystem(
    rowHeight: Dp,
    isShowingSystemDistance: Boolean,
    isUsingJumpBridges: Boolean,
    system: String,
) {
    IntelSystem(
        system = system,
        rowHeight = rowHeight,
        isShowingSystemDistance = isShowingSystemDistance,
        isUsingJumpBridges = isUsingJumpBridges,
    )
}

@Composable
private fun TokenWithGate(rowHeight: Dp, fromSystem: String?, toSystem: String, isAnsiblex: Boolean) {
    val systemsRepository: SolarSystemsRepository by koin.inject()

    BorderedToken(rowHeight) {
        GateIcon(
            isAnsiblex = isAnsiblex,
            fromSystem = fromSystem,
            toSystem = toSystem,
            size = rowHeight,
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        val sunTypeId = systemsRepository.getSystemSunTypeId(toSystem)
        AsyncTypeIcon(
            typeId = sunTypeId,
            modifier = Modifier.size(rowHeight),
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        val gateText = if (isAnsiblex) "Ansiblex" else "Gate"
        Text(
            text = "$toSystem $gateText",
            style = RiftTheme.typography.bodyLink,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithMovement(rowHeight: Dp, previousTokens: List<Token>, movement: TokenType.Movement) {
    val systemsRepository: SolarSystemsRepository by koin.inject()
    BorderedToken(rowHeight) {
        Image(
            painter = painterResource(Res.drawable.keywords_systems),
            contentDescription = null,
            modifier = Modifier.size(rowHeight),
        )
        if (movement.isGate) {
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            val systemFrom = previousTokens.mapNotNull { it.types.filterIsInstance<TokenType.System>().firstOrNull() }.lastOrNull()?.name
            GateIcon(
                isAnsiblex = false,
                fromSystem = systemFrom,
                toSystem = movement.toSystem,
                size = rowHeight,
            )
        }
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        val sunTypeId = systemsRepository.getSystemSunTypeId(movement.toSystem)
        AsyncTypeIcon(
            typeId = sunTypeId,
            modifier = Modifier.size(rowHeight),
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        Text(
            text = movement.verb,
            modifier = Modifier.padding(vertical = 4.dp).padding(start = 4.dp),
        )
        Text(
            text = movement.toSystem,
            style = RiftTheme.typography.bodyLink,
            modifier = Modifier.padding(4.dp),
        )
        if (movement.isGate) {
            Text(
                text = "gate",
                modifier = Modifier.padding(vertical = 4.dp).padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun TokenWithKeyword(rowHeight: Dp, type: KeywordType) {
    BorderedToken(rowHeight) {
        when (type) {
            KeywordType.NoVisual -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_no_visual),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "No visual",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Clear -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_clear),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Clear",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Wormhole -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_wormhole),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Wormhole",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Spike -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_spike),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Spike",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Ess -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_ess),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "ESS",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Skyhook -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_skyhook),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Skyhook",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.GateCamp -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_gatecamp),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Gate Camp",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.CombatProbes -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_combat_probe),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Combat Probes",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Bubbles -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_interdiction_probe),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Bubbles",
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun TokenWithPlayer(rowHeight: Dp, name: String, characterId: Int) {
    ClickablePlayer(characterId) {
        BorderedToken(rowHeight) {
            AsyncPlayerPortrait(
                characterId = characterId,
                size = 32,
                modifier = Modifier.size(rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            Text(
                text = name,
                style = RiftTheme.typography.bodyHighlighted,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun TokenWithKill(rowHeight: Dp, token: TokenType.Kill) {
    val repository: TypesRepository by koin.inject()
    RiftTooltipArea(
        text = "${token.name}\n${token.target}",
    ) {
        ClickablePlayer(token.characterId) {
            BorderedToken(rowHeight) {
                Image(
                    painter = painterResource(Res.drawable.keywords_killreport),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
                AsyncPlayerPortrait(
                    characterId = token.characterId,
                    size = 32,
                    modifier = Modifier.size(rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
                val type = repository.getType(token.target)
                AsyncTypeIcon(
                    type = type,
                    modifier = Modifier.size(rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
                Text(
                    text = token.name,
                    style = RiftTheme.typography.bodyHighlighted,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ParsedMessagePreview() {
    fun String.token(vararg types: TokenType): Token {
        return Token(split(" "), types = types.toList())
    }
    val settings = IntelReportsSettings(
        displayTimezone = ZoneId.systemDefault(),
        isUsingCompactMode = false,
        isShowingReporter = true,
        isShowingChannel = true,
        isShowingRegion = true,
        isShowingSystemDistance = true,
        isUsingJumpBridgesForDistance = true,
    )

    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            val message = ParsedChannelChatMessage(
                ChatMessage(Instant.now(), "Player One", ""),
                "Delve",
                ChatLogFileMetadata("", "Intel", "", 0, "", null, null),
                listOf(
                    "ssllss1".token(TokenType.Player(1), Link),
                    "Yaakov Y2".token(TokenType.Player(1)),
                    "2x".token(TokenType.Count(2)),
                    "capsule".token(TokenType.Ship("Capsule"), Link),
                    "319-3D".token(TokenType.System("319-3D")),
                ),
            )
            ChatMessage(settings, message, null)

            val message2 = ParsedChannelChatMessage(
                ChatMessage(Instant.now(), "Player Two", ""),
                "Delve",
                ChatLogFileMetadata("", "Intel", "", 0, "", null, null),
                listOf(
                    "ssllss1".token(TokenType.Player(1), Link),
                    "Yaakov Y2".token(TokenType.Player(1)),
                    "very long text that will not fit".token(),
                    "2x".token(TokenType.Count(2)),
                    "capsule".token(TokenType.Ship("Capsule"), Link),
                    "319-3D".token(TokenType.System("319-3D")),
                ),
            )
            ChatMessage(settings, message2, null)

            val message3 = ParsedChannelChatMessage(
                ChatMessage(Instant.now(), "Player Three", ""),
                "Delve",
                ChatLogFileMetadata("", "Intel", "", 0, "", null, null),
                listOf(
                    "ssllss1".token(TokenType.Player(1), Link),
                    "Yaakov Y2".token(TokenType.Player(1)),
                    "very long text that will not fit even on a whole separate line, but will make the pill multiline".token(),
                    "2x".token(TokenType.Count(2)),
                    "capsule".token(TokenType.Ship("Capsule"), Link),
                    "319-3D".token(TokenType.System("319-3D")),
                ),
            )
            ChatMessage(settings, message3, null)
        }
    }
}
