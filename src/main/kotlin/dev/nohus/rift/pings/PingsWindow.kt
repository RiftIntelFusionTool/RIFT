package dev.nohus.rift.pings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftOpportunityBox
import dev.nohus.rift.compose.RiftOpportunityBoxButton
import dev.nohus.rift.compose.RiftOpportunityBoxCategory
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SolarSystemPillState
import dev.nohus.rift.compose.TextWithLinks
import dev.nohus.rift.compose.annotateLinks
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.fitting_16px
import dev.nohus.rift.generated.resources.microphone
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.pings.PingsViewModel.UiState
import dev.nohus.rift.utils.Clipboard
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager
import java.time.ZoneId

@Composable
fun PingsWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: PingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Pings",
        icon = Res.drawable.window_sovereignty,
        state = windowState,
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        PingsWindowContent(
            state = state,
            onOpenJabberClick = viewModel::onOpenJabberClick,
            onMumbleClick = viewModel::onMumbleClick,
        )
    }
}

@Composable
private fun PingsWindowContent(
    state: UiState,
    onOpenJabberClick: () -> Unit,
    onMumbleClick: (url: String) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        val scrollState = rememberScrollState()
        LaunchedEffect(state.pings) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        ScrollbarColumn(
            scrollState = scrollState,
            contentPadding = PaddingValues(start = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            scrollbarModifier = Modifier.padding(horizontal = Spacing.small),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = Spacing.medium),
        ) {
            state.pings.forEach { ping ->
                when (ping) {
                    is PingUiModel.PlainText -> PlainTextPing(state.displayTimezone, ping)
                    is PingUiModel.FleetPing -> FleetPing(state.displayTimezone, ping, onMumbleClick)
                }
            }
        }
        if (state.pings.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val text = if (state.isJabberConnected) {
                    "No pings received yet.\nClear skies."
                } else {
                    "You need to be connected to Jabber to receive pings."
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.titlePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                )
                if (!state.isJabberConnected) {
                    RiftButton(
                        text = "Check Jabber",
                        onClick = onOpenJabberClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlainTextPing(
    displayTimezone: ZoneId,
    ping: PingUiModel.PlainText,
) {
    val type = buildAnnotatedString {
        if (ping.target == null || ping.target == "all") {
            append("Announcement")
        } else {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.target.replaceFirstChar { it.uppercase() })
            }
            append(" message")
        }
        if (ping.sender != null) {
            append(" from ")
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.sender)
            }
        }
    }
    val buttons = mutableListOf<RiftOpportunityBoxButton>()
    buttons += RiftOpportunityBoxButton(
        resource = Res.drawable.copy_16px,
        tooltip = "Copy ping",
        action = { Clipboard.copy(ping.sourceText) },
    )
    RiftOpportunityBox(
        category = RiftOpportunityBoxCategory.Unclassified,
        type = type,
        locations = emptyList(),
        character = null,
        title = null,
        timestamp = ping.timestamp,
        displayTimezone = displayTimezone,
        buttons = buttons,
    ) {
        val descriptionStyle = if (ping.text.length <= 50) {
            RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold)
        } else {
            RiftTheme.typography.bodyPrimary
        }
        val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
        val linkifiedMessage = remember(ping.text) { annotateLinks(ping.text, linkStyle) }
        TextWithLinks(
            text = linkifiedMessage,
            style = descriptionStyle,
        )
    }
}

@Composable
private fun FleetPing(
    displayTimezone: ZoneId,
    ping: PingUiModel.FleetPing,
    onMumbleClick: (url: String) -> Unit,
) {
    val type = buildAnnotatedString {
        if (ping.target == null || ping.target == "all") {
            append("Fleet")
        } else {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.target.replaceFirstChar { it.uppercase() })
            }
            append(" fleet")
        }
        if (ping.fleet != null) {
            append(" ")
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.fleet)
            }
        }
        append(" under ")
        withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
            append(ping.fleetCommander.name)
        }
    }
    val buttons = mutableListOf<RiftOpportunityBoxButton>()
    if (ping.doctrine?.link != null) {
        buttons += RiftOpportunityBoxButton(
            resource = Res.drawable.fitting_16px,
            tooltip = "Doctrine forum thread",
            action = { ping.doctrine.link.toURIOrNull()?.openBrowser() },
        )
    }
    buttons += RiftOpportunityBoxButton(
        resource = Res.drawable.copy_16px,
        tooltip = "Copy ping",
        action = { Clipboard.copy(ping.sourceText) },
    )
    if (ping.comms is Comms.Mumble) {
        buttons += RiftOpportunityBoxButton(
            resource = Res.drawable.microphone,
            tooltip = "Join ${ping.comms.channel} on Mumble",
            action = { onMumbleClick(ping.comms.link) },
        )
    }
    val title = when (ping.papType) {
        PapType.Peacetime -> "Peacetime PAP"
        PapType.Strategic -> "Strategic PAP"
        is PapType.Text -> "${ping.papType.text.replaceFirstChar { it.uppercase() }} PAP"
        null -> "No PAP"
    }
    RiftOpportunityBox(
        category = ping.opportunityCategory,
        type = type,
        locations = ping.formupLocations.map { getSolarSystemPillState(it) },
        character = ping.fleetCommander,
        title = title,
        timestamp = ping.timestamp,
        displayTimezone = displayTimezone,
        buttons = buttons,
    ) {
        val descriptionStyle = if (ping.description.length <= 50) {
            RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold)
        } else {
            RiftTheme.typography.bodyPrimary
        }
        val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
        val linkifiedMessage = remember(ping.description) { annotateLinks(ping.description, linkStyle) }
        TextWithLinks(
            text = linkifiedMessage,
            style = descriptionStyle,
            modifier = Modifier.padding(top = Spacing.mediumLarge),
        )
        if (ping.comms is Comms.Text) {
            Text(
                text = "Comms:",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(top = Spacing.mediumLarge),
            )
            val linkifiedComms = remember(ping.comms.text) { annotateLinks(ping.comms.text, linkStyle) }
            TextWithLinks(
                text = linkifiedComms,
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        if (ping.doctrine != null) {
            Text(
                text = "Doctrine:",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(top = Spacing.mediumLarge),
            )
            Text(
                text = ping.doctrine.text,
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
}

private fun getSolarSystemPillState(location: FormupLocationUiModel): SolarSystemPillState {
    return when (location) {
        is FormupLocationUiModel.System -> {
            SolarSystemPillState(distance = location.distance, name = location.name, security = location.security)
        }
        is FormupLocationUiModel.Text -> SolarSystemPillState(distance = null, name = location.text, security = null)
    }
}
