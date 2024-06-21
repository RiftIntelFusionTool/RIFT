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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
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
import dev.nohus.rift.generated.resources.keywords_spike
import dev.nohus.rift.generated.resources.keywords_systems
import dev.nohus.rift.generated.resources.keywords_wormhole
import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.settings.IntelReportsSettings
import dev.nohus.rift.logs.parse.ChatLogFileMetadata
import dev.nohus.rift.logs.parse.ChatMessage
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType
import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Link
import dev.nohus.rift.repositories.GetSystemDistanceFromCharacterUseCase
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
        tooltip = "Original message:\n${message.chatMessage.message}",
        anchor = TooltipAnchor.BottomStart,
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
                    is TokenType.Count -> TokenWithCount(settings, text)
                    is TokenType.Keyword -> TokenWithKeyword(settings, type.type)
                    is TokenType.Kill -> TokenWithKill(settings, type)
                    Link -> TokenWithText(settings, text, "Link")
                    is TokenType.Player -> TokenWithPlayer(settings, text, type.characterId)
                    is TokenType.Question -> TokenWithText(settings, text, "Question")
                    is TokenType.Ship -> TokenWithShip(settings, type)
                    is TokenType.System -> TokenWithSystem(settings, type.name)
                    TokenType.Url -> TokenWithUrl(settings, text)
                    is TokenType.Gate -> {
                        val fromSystem = tokens.mapNotNull { it.types.filterIsInstance<TokenType.System>().firstOrNull() }.singleOrNull()?.name
                        TokenWithGate(settings, fromSystem, type.system, type.isAnsiblex)
                    }
                    is TokenType.Movement -> TokenWithMovement(settings, tokens, type)
                }
            } else if (types.size > 1) {
                TokenWithText(settings, text, "Multi")
            } else {
                TokenWithPlainText(settings, text)
            }
        }
    }
}

@Composable
private fun BorderedToken(
    settings: IntelReportsSettings,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = settings.rowHeight).border(1.dp, RiftTheme.colors.borderGreyLight),
    ) {
        content()
    }
}

@Composable
private fun TokenWithText(settings: IntelReportsSettings, text: String, type: String) {
    BorderedToken(settings) {
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
private fun TokenWithCount(settings: IntelReportsSettings, text: String) {
    BorderedToken(settings) {
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithPlainText(settings: IntelReportsSettings, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = settings.rowHeight),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TokenWithUrl(settings: IntelReportsSettings, text: String) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = settings.rowHeight)
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
private fun TokenWithShip(settings: IntelReportsSettings, ship: TokenType.Ship) {
    val repository: ShipTypesRepository by koin.inject()
    val shipTypeId = repository.getShipTypeId(ship.name)
    ClickableShip(ship.name, shipTypeId) {
        BorderedToken(settings) {
            AsyncTypeIcon(
                typeId = shipTypeId,
                modifier = Modifier.size(settings.rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
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
private fun TokenWithSystem(settings: IntelReportsSettings, system: String) {
    val repository: SolarSystemsRepository by koin.inject()
    ClickableSystem(system) {
        BorderedToken(settings) {
            val sunTypeId = repository.getSystemSunTypeId(system)
            AsyncTypeIcon(
                typeId = sunTypeId,
                modifier = Modifier.size(settings.rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
            Text(
                text = system,
                style = RiftTheme.typography.bodyLink,
                modifier = Modifier.padding(4.dp),
            )
            if (settings.isShowingSystemDistance) {
                val systemId = repository.getSystemId(system) ?: return@BorderedToken
                SystemDistanceIndicator(systemId, settings.rowHeight)
            }
        }
    }
}

@Composable
private fun SystemDistanceIndicator(
    systemId: Int,
    height: Dp,
) {
    val getDistance: GetSystemDistanceFromCharacterUseCase by koin.inject()
    val distance = getDistance(systemId)
    if (distance > 5) return
    val distanceColor = getDistanceColor(distance)
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(height)
            .padding(top = 1.dp, bottom = 1.dp, end = 1.dp)
            .border(2.dp, distanceColor, RoundedCornerShape(100))
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = "$distance",
            style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier,
        )
    }
}

private fun getDistanceColor(distance: Int): Color {
    return when {
        distance >= 5 -> Color(0xFF2E74DF)
        distance >= 4 -> Color(0xFF4ACFF3)
        distance >= 3 -> Color(0xFF5CDCA6)
        distance >= 2 -> Color(0xFF70E552)
        distance >= 1 -> Color(0xFFDC6C08)
        else -> Color(0xFFBC1113)
    }
}

@Composable
private fun TokenWithGate(settings: IntelReportsSettings, fromSystem: String?, toSystem: String, isAnsiblex: Boolean) {
    val systemsRepository: SolarSystemsRepository by koin.inject()

    BorderedToken(settings) {
        GateIcon(
            isAnsiblex = isAnsiblex,
            fromSystem = fromSystem,
            toSystem = toSystem,
            size = settings.rowHeight,
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
        val sunTypeId = systemsRepository.getSystemSunTypeId(toSystem)
        AsyncTypeIcon(
            typeId = sunTypeId,
            modifier = Modifier.size(settings.rowHeight),
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
        val gateText = if (isAnsiblex) "Ansiblex" else "Gate"
        Text(
            text = "$toSystem $gateText",
            style = RiftTheme.typography.bodyLink,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithMovement(settings: IntelReportsSettings, previousTokens: List<Token>, movement: TokenType.Movement) {
    val systemsRepository: SolarSystemsRepository by koin.inject()
    BorderedToken(settings) {
        Image(
            painter = painterResource(Res.drawable.keywords_systems),
            contentDescription = null,
            modifier = Modifier.size(settings.rowHeight),
        )
        if (movement.isGate) {
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
            val systemFrom = previousTokens.mapNotNull { it.types.filterIsInstance<TokenType.System>().firstOrNull() }.lastOrNull()?.name
            GateIcon(
                isAnsiblex = false,
                fromSystem = systemFrom,
                toSystem = movement.toSystem,
                size = settings.rowHeight,
            )
        }
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
        val sunTypeId = systemsRepository.getSystemSunTypeId(movement.toSystem)
        AsyncTypeIcon(
            typeId = sunTypeId,
            modifier = Modifier.size(settings.rowHeight),
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
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
private fun TokenWithKeyword(settings: IntelReportsSettings, type: KeywordType) {
    BorderedToken(settings) {
        when (type) {
            KeywordType.NoVisual -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_no_visual),
                    contentDescription = null,
                    modifier = Modifier.size(settings.rowHeight),
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
                    modifier = Modifier.size(settings.rowHeight),
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
                    modifier = Modifier.size(settings.rowHeight),
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
                    modifier = Modifier.size(settings.rowHeight),
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
                    modifier = Modifier.size(settings.rowHeight),
                )
                Text(
                    text = "ESS",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.GateCamp -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_gatecamp),
                    contentDescription = null,
                    modifier = Modifier.size(settings.rowHeight),
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
                    modifier = Modifier.size(settings.rowHeight),
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
                    modifier = Modifier.size(settings.rowHeight),
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
private fun TokenWithPlayer(settings: IntelReportsSettings, name: String, characterId: Int) {
    ClickablePlayer(characterId) {
        BorderedToken(settings) {
            AsyncPlayerPortrait(
                characterId = characterId,
                size = 32,
                modifier = Modifier.size(settings.rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
            Text(
                text = name,
                style = RiftTheme.typography.bodyHighlighted,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun TokenWithKill(settings: IntelReportsSettings, token: TokenType.Kill) {
    val repository: TypesRepository by koin.inject()
    RiftTooltipArea(
        tooltip = "${token.name}\n${token.target}",
        anchor = TooltipAnchor.BottomCenter,
    ) {
        ClickablePlayer(token.characterId) {
            BorderedToken(settings) {
                Image(
                    painter = painterResource(Res.drawable.keywords_killreport),
                    contentDescription = null,
                    modifier = Modifier.size(settings.rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
                AsyncPlayerPortrait(
                    characterId = token.characterId,
                    size = 32,
                    modifier = Modifier.size(settings.rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
                val typeId = repository.getTypeId(token.target)
                AsyncTypeIcon(
                    typeId = typeId,
                    modifier = Modifier.size(settings.rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(settings.rowHeight))
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
