package dev.nohus.rift.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AnimatedImage
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.partner_400
import dev.nohus.rift.generated.resources.tray_tray_64
import dev.nohus.rift.generated.resources.window_agent
import dev.nohus.rift.get
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import dev.nohus.rift.wizard.WizardViewModel.EveInstallationState
import dev.nohus.rift.wizard.WizardViewModel.UiState
import dev.nohus.rift.wizard.WizardViewModel.WizardStep
import org.jetbrains.compose.resources.painterResource

@Composable
fun WizardWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: WizardViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "RIFT Intel Fusion Tool",
        icon = Res.drawable.window_agent,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        WizardWindowContent(
            state = state,
            onSetEveInstallationClick = viewModel::onSetEveInstallationClick,
            onCharactersClick = viewModel::onCharactersClick,
            onConfigurationPackChange = viewModel::onConfigurationPackChange,
            onSetIntelChannelsClick = viewModel::onSetIntelChannelsClick,
            onContinueClick = viewModel::onContinueClick,
            onKeyEvent = viewModel::onKeyEvent,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }

        if (state.onFinishedEvent.get()) onCloseRequest()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WizardWindowContent(
    state: UiState,
    onSetEveInstallationClick: () -> Unit,
    onCharactersClick: () -> Unit,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    onSetIntelChannelsClick: () -> Unit,
    onContinueClick: () -> Unit,
    onKeyEvent: (KeyEvent) -> Unit,
) {
    val focusRequester = FocusRequester()
    Row(
        modifier = Modifier
            .onKeyEvent {
                onKeyEvent(it)
                false
            }
            .focusRequester(focusRequester)
            .focusable()
            .onClick { focusRequester.requestFocus() },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(end = Spacing.large),
        ) {
            AnimatedImage(
                resource = "files/aura.gif",
                modifier = Modifier
                    .size(200.dp)
                    .border(2.dp, RiftTheme.colors.borderPrimary),
            )
            Image(
                painter = painterResource(Res.drawable.partner_400),
                contentDescription = null,
                modifier = Modifier
                    .width(200.dp),
            )
        }
        when (val step = state.step) {
            WizardStep.Welcome -> WelcomeStep(
                onContinueClick = onContinueClick,
            )
            is WizardStep.EveInstallation -> EveInstallationStep(
                step = step,
                onSetEveInstallationClick = onSetEveInstallationClick,
                onContinueClick = onContinueClick,
            )
            is WizardStep.Characters -> CharactersStep(
                step = step,
                onCharactersClick = onCharactersClick,
                onContinueClick = onContinueClick,
            )
            is WizardStep.ConfigurationPacks -> ConfigurationPacksStep(
                step = step,
                onConfigurationPackChange = onConfigurationPackChange,
                onContinueClick = onContinueClick,
            )
            is WizardStep.IntelChannels -> IntelChannelsStep(
                step = step,
                onSetIntelChannelsClick = onSetIntelChannelsClick,
                onContinueClick = onContinueClick,
            )
            WizardStep.Finish -> FinishStep(
                onContinueClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append("Welcome, capsuleer!")
            }
            append(
                "\n\nYou are now in possession of RIFT, a prototype military system that will " +
                    "aid your situational awareness with additional intel.\n\n" +
                    "I will get you started.",
            )
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.titlePrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
    }
}

@Composable
private fun EveInstallationStep(
    step: WizardStep.EveInstallation,
    onSetEveInstallationClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    val isContinueWarning = when (step.state) {
        EveInstallationState.None -> true
        EveInstallationState.Detected -> false
        EveInstallationState.Set -> false
    }
    var hasFinishedTyping by remember { mutableStateOf(false) }

    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = isContinueWarning,
    ) {
        val text = buildAnnotatedString {
            when (step.state) {
                EveInstallationState.None -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("EVE Online not detected")
                    }
                    append(
                        "\n\nTo set up RIFT properly, I need to know the location of your EVE installation.",
                    )
                }
                EveInstallationState.Detected -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("EVE Online detected")
                    }
                    append(
                        "\n\nI have located your installation of EVE.",
                    )
                }
                EveInstallationState.Set -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("EVE Online located")
                    }
                    append(
                        "\n\nYour installation of EVE is now set up correctly.",
                    )
                }
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.titlePrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = when (step.state) {
                EveInstallationState.None -> ButtonType.Primary to "Select installation"
                EveInstallationState.Detected -> ButtonType.Secondary to "Check installation"
                EveInstallationState.Set -> ButtonType.Secondary to "Change installation"
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onSetEveInstallationClick,
            )
        }
    }
}

@Composable
private fun CharactersStep(
    step: WizardStep.Characters,
    onCharactersClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = step.authenticatedCharacterCount < step.characterCount,
    ) {
        val text = buildAnnotatedString {
            if (step.characterCount == 0) {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append("No characters detected")
                }
                append(
                    "\n\nI did not detect any characters that were used on this device.",
                )
            } else {
                if (step.authenticatedCharacterCount == 0) {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("Characters detected")
                    }
                    append(
                        "\n\nI have detected ${step.characterCount} of your characters.\n\n" +
                            "Let's authenticate them in order to use them with RIFT.",
                    )
                } else if (step.authenticatedCharacterCount < step.characterCount) {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("Characters partially set up")
                    }
                    append(
                        "\n\nI have detected ${step.characterCount} of your characters, " +
                            "but you have only authenticated ${step.authenticatedCharacterCount} of them.",
                    )
                } else {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("Characters set up")
                    }
                    append(
                        "\n\nAll of your characters are authenticated and ready to use with RIFT.",
                    )
                }
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.titlePrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = if (step.authenticatedCharacterCount < step.characterCount) {
                ButtonType.Primary to "Authenticate characters"
            } else {
                ButtonType.Secondary to "Check characters"
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onCharactersClick,
            )
        }
    }
}

@Composable
private fun ConfigurationPacksStep(
    step: WizardStep.ConfigurationPacks,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    val getPackName: (ConfigurationPack?) -> String = { it.displayName }
    StepContent(
        onContinueClick = onContinueClick,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append("Alliance features")
            }
            if (step.pack != null) {
                append(
                    "\n\nDo you want to enable features specific to ${getPackName(step.pack)}?",
                )
            } else {
                append(
                    "\n\nYou selected default settings.",
                )
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.titlePrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            RiftDropdownWithLabel(
                label = "Configuration pack:",
                items = listOf(null) + ConfigurationPack.entries,
                selectedItem = step.pack,
                onItemSelected = onConfigurationPackChange,
                getItemName = getPackName,
            )
        }
    }
}

@Composable
private fun IntelChannelsStep(
    step: WizardStep.IntelChannels,
    onSetIntelChannelsClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = !step.hasChannels,
    ) {
        val text = buildAnnotatedString {
            if (step.hasChannels) {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append("Intel channels set up")
                }
                append(
                    "\n\nYou have configured your intel channels.",
                )
            } else {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append("Intel channels")
                }
                append(
                    "\n\nLet's add intel channels that you want RIFT to monitor for intel reports.",
                )
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.titlePrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = if (!step.hasChannels) {
                ButtonType.Primary to "Add intel channels"
            } else {
                ButtonType.Secondary to "Change intel channels"
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onSetIntelChannelsClick,
            )
        }
    }
}

@Composable
private fun FinishStep(
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append("All done!")
            }
            append(
                "\n\nRIFT is now ready to enhance your capabilities. Look for this tray icon to access all features:",
            )
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.titlePrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large),
        ) {
            Image(
                painter = painterResource(Res.drawable.tray_tray_64),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun StepContent(
    onContinueClick: () -> Unit,
    isContinueVisible: Boolean = true,
    isWarning: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = isContinueVisible,
            enter = fadeIn(),
            modifier = Modifier.align(Alignment.End),
        ) {
            val text = if (isWarning) "Continue anyway" else "Continue"
            val type = if (isWarning) ButtonType.Negative else ButtonType.Primary
            RiftButton(
                text = text,
                type = type,
                onClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun TypingText(
    text: AnnotatedString,
    style: TextStyle,
    onFinishedTyping: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var targetValue by remember(text) { mutableStateOf(0) }
    val animationSpec = remember(targetValue) {
        tween<Int>(durationMillis = targetValue * 20, easing = LinearEasing)
    }
    val typedCharacters = key(text) { animateIntAsState(targetValue, animationSpec, finishedListener = { onFinishedTyping() }) }
    LaunchedEffect(text) {
        targetValue = text.length
    }
    Text(
        text = text.subSequence(0, typedCharacters.value),
        style = style,
        modifier = modifier,
    )
}
