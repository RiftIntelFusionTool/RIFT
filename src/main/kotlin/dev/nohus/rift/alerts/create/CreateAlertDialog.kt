package dev.nohus.rift.alerts.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.alerts.create.CreateAlertViewModel.UiState
import dev.nohus.rift.alerts.create.FormAnswer.CharacterAnswer
import dev.nohus.rift.alerts.create.FormAnswer.FreeformTextAnswer
import dev.nohus.rift.alerts.create.FormAnswer.IntelChannelAnswer
import dev.nohus.rift.alerts.create.FormAnswer.JumpsRangeAnswer
import dev.nohus.rift.alerts.create.FormAnswer.MultipleChoiceAnswer
import dev.nohus.rift.alerts.create.FormAnswer.PlanetaryIndustryColoniesAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SingleChoiceAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SoundAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SpecificCharactersAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SystemAnswer
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckbox
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftFileChooserButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftRadioButton
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.play
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.get
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.utils.sound.Sound
import dev.nohus.rift.utils.sound.SoundPlayer
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.nameWithoutExtension

@Composable
fun WindowScope.CreateAlertDialog(
    inputModel: CreateAlertInputModel,
    parentWindowState: WindowManager.RiftWindowState,
    onDismiss: () -> Unit,
) {
    val viewModel: CreateAlertViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    if (state.dismissEvent.get()) onDismiss()

    val title = when (inputModel) {
        CreateAlertInputModel.New -> "New alert"
        is CreateAlertInputModel.EditAction -> "Edit alert action"
    }
    RiftDialog(
        title = title,
        icon = Res.drawable.window_loudspeaker_icon,
        parentState = parentWindowState,
        state = rememberWindowState(width = 400.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        CreateAlertDialogContent(
            state = state,
            onFormPendingAnswer = viewModel::onFormPendingAnswer,
            onBackClick = viewModel::onBackClick,
            onContinueClick = viewModel::onContinueClick,
        )
    }
}

@Composable
private fun CreateAlertDialogContent(
    state: UiState,
    onFormPendingAnswer: (FormAnswer) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    Column {
        if (state.formAnswers.isNotEmpty()) {
            Column {
                for ((question, answer) in state.formAnswers) {
                    val answerText = (question to answer).toAnswerString(state.characters)
                    if (answerText != null) {
                        val text = buildAnnotatedString {
                            append(question.title)
                            append(" ")
                            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                                append(answerText)
                            }
                        }
                        Text(
                            text = text,
                            style = RiftTheme.typography.bodySecondary,
                            modifier = Modifier.padding(bottom = Spacing.small),
                        )
                    }
                }
            }
        }
        Column {
            AnimatedContent(state.formQuestion) { formQuestion ->
                if (formQuestion != null) {
                    val highlightAnimationState = remember { AnimationState(0f) }
                    LaunchedEffect(state.highlightQuestionEvent) {
                        if (state.highlightQuestionEvent.get()) {
                            highlightAnimationState.animateTo(1f, animationSpec = tween(300))
                            highlightAnimationState.animateTo(0f, animationSpec = tween(300))
                        }
                    }
                    Box(
                        modifier = Modifier.alpha(1f - (0.5f * highlightAnimationState.value)),
                    ) {
                        FormQuestion(
                            formQuestion = formQuestion,
                            isPendingAnswerValid = state.isPendingAnswerValid,
                            pendingAnswerInvalidReason = state.pendingAnswerInvalidReason,
                            characters = state.characters,
                            intelChannels = state.intelChannels,
                            sounds = state.sounds,
                            recentTargets = state.recentTargets,
                            colonies = state.colonies,
                            onFormAnswer = onFormPendingAnswer,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(top = Spacing.medium),
            ) {
                RiftButton(
                    text = "Back",
                    cornerCut = ButtonCornerCut.BottomLeft,
                    type = ButtonType.Secondary,
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                )
                val label = if (state.formQuestion != null) "Continue" else "Finish"
                RiftButton(
                    text = label,
                    onClick = onContinueClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FormQuestion(
    formQuestion: FormQuestion,
    isPendingAnswerValid: Boolean?,
    pendingAnswerInvalidReason: String?,
    characters: List<LocalCharacter>,
    intelChannels: List<String>,
    sounds: List<Sound>,
    recentTargets: Set<String>,
    colonies: List<ColonyItem>,
    onFormAnswer: (FormAnswer) -> Unit,
) {
    Column {
        Text(
            text = formQuestion.title,
            style = RiftTheme.typography.titleSecondary,
            modifier = Modifier
                .padding(vertical = Spacing.medium),
        )
        when (formQuestion) {
            is FormQuestion.SingleChoiceQuestion -> {
                ScrollbarColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                ) {
                    var selected: FormChoiceItem? by remember { mutableStateOf(null) }
                    for (item in formQuestion.items) {
                        ListSelectorRow(
                            text = item.text,
                            description = item.description,
                            isMultipleChoice = false,
                            isSelected = item == selected,
                            onSelect = {
                                selected = item
                                onFormAnswer(SingleChoiceAnswer(item.id))
                            },
                        )
                    }
                }
            }

            is FormQuestion.MultipleChoiceQuestion -> {
                ScrollbarColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                ) {
                    var selected: List<FormChoiceItem> by remember { mutableStateOf(emptyList()) }
                    for (item in formQuestion.items) {
                        ListSelectorRow(
                            text = item.text,
                            description = item.description,
                            isMultipleChoice = true,
                            isSelected = item in selected,
                            onSelect = {
                                if (item in selected) selected -= item else selected += item
                                onFormAnswer(MultipleChoiceAnswer(selected))
                            },
                        )
                    }
                }
            }

            is FormQuestion.SystemQuestion -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 36.dp) // For requirement icon
                        .fillMaxWidth(),
                ) {
                    var system: String by remember { mutableStateOf("") }
                    val placeholder = if (formQuestion.allowEmpty) {
                        "System name, or leave empty"
                    } else {
                        "System name"
                    }
                    if (formQuestion.allowEmpty) {
                        LaunchedEffect(Unit) {
                            onFormAnswer(SystemAnswer(""))
                        }
                    }
                    RiftTextField(
                        text = system,
                        placeholder = placeholder,
                        onTextChanged = {
                            system = it
                            onFormAnswer(SystemAnswer(it))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isPendingAnswerValid != null) {
                        RequirementIcon(
                            isFulfilled = isPendingAnswerValid,
                            fulfilledTooltip = "System valid",
                            notFulfilledTooltip = "System does not exist",
                            modifier = Modifier.padding(start = Spacing.medium),
                        )
                    }
                }
            }

            is FormQuestion.JumpsRangeQuestion -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    var min: Int by remember { mutableStateOf(0) }
                    var max: Int by remember { mutableStateOf(0) }
                    val itemNames = listOf("Same system", "1 jump", "2 jumps", "3 jumps", "4 jumps", "5 jumps")
                    LaunchedEffect(formQuestion) {
                        onFormAnswer(JumpsRangeAnswer(minJumps = min, maxJumps = max))
                    }
                    Text(
                        text = "From: ",
                        style = RiftTheme.typography.titlePrimary,
                    )
                    RiftDropdown(
                        items = List(6) { it },
                        selectedItem = min,
                        onItemSelected = {
                            min = it
                            if (max < min) max = min
                            onFormAnswer(JumpsRangeAnswer(minJumps = min, maxJumps = max))
                        },
                        getItemName = { itemNames[it] },
                        maxItems = 3,
                    )
                    Text(
                        text = " To: ",
                        style = RiftTheme.typography.titlePrimary,
                    )
                    RiftDropdown(
                        items = List(6 - min) { min + it },
                        selectedItem = max,
                        onItemSelected = {
                            max = it
                            onFormAnswer(JumpsRangeAnswer(minJumps = min, maxJumps = max))
                        },
                        getItemName = { itemNames[it] },
                        maxItems = 3,
                    )
                }
            }

            is FormQuestion.OwnedCharacterQuestion -> {
                if (characters.isNotEmpty()) {
                    var selected: Int by remember { mutableStateOf(characters.first().characterId) }
                    LaunchedEffect(formQuestion) {
                        onFormAnswer(CharacterAnswer(selected))
                    }
                    RiftDropdown(
                        items = characters.map { it.characterId },
                        selectedItem = selected,
                        onItemSelected = { characterId ->
                            selected = characterId
                            onFormAnswer(CharacterAnswer(characterId))
                        },
                        getItemName = { characterId ->
                            characters.firstOrNull { it.characterId == characterId }?.info?.success?.name ?: "$characterId"
                        },
                    )
                } else {
                    Text(
                        text = "You have no characters to choose from!",
                        style = RiftTheme.typography.titlePrimary,
                    )
                }
            }

            is FormQuestion.IntelChannelQuestion -> {
                if (intelChannels.isNotEmpty()) {
                    var selected: String by remember { mutableStateOf(intelChannels.first()) }
                    LaunchedEffect(formQuestion) {
                        onFormAnswer(IntelChannelAnswer(selected))
                    }
                    RiftDropdown(
                        items = intelChannels,
                        selectedItem = selected,
                        onItemSelected = {
                            selected = it
                            onFormAnswer(IntelChannelAnswer(it))
                        },
                        getItemName = { it },
                    )
                } else {
                    Text(
                        text = "You have no intel channels to choose from!",
                        style = RiftTheme.typography.titlePrimary,
                    )
                }
            }

            is FormQuestion.SoundQuestion -> {
                val soundPlayer: SoundPlayer = remember { koin.get() }
                var selected: Sound? by remember { mutableStateOf(null) }
                var selectedTabIndex by remember { mutableStateOf(0) }
                RiftTabBar(
                    tabs = listOf(
                        Tab(0, "Built-in sounds", isCloseable = false),
                        Tab(1, "Custom sound", isCloseable = false),
                    ),
                    selectedTab = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    onTabClosed = {},
                )
                ScrollbarColumn(
                    modifier = Modifier.height(180.dp),
                ) {
                    if (selectedTabIndex == 0) {
                        for (sound in sounds) {
                            ListSelectorRow(
                                text = sound.name,
                                description = null,
                                isMultipleChoice = false,
                                isSelected = sound == selected,
                                onSelect = {
                                    selected = sound
                                    onFormAnswer(SoundAnswer.BuiltInSound(sound))
                                },
                                rightContent = {
                                    RiftImageButton(
                                        Res.drawable.play,
                                        size = 16.dp,
                                        onClick = { soundPlayer.play(sound.resource) },
                                        modifier = Modifier.padding(horizontal = Spacing.small),
                                    )
                                },
                            )
                        }
                    } else {
                        Text(
                            text = "Choose a sound file:",
                            style = RiftTheme.typography.bodyPrimary,
                            modifier = Modifier.padding(top = Spacing.medium),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier
                                .padding(top = Spacing.medium)
                                .heightIn(min = 36.dp),
                        ) {
                            var text by remember { mutableStateOf("") }
                            RiftTextField(
                                text = text,
                                onTextChanged = {
                                    text = it
                                    onFormAnswer(SoundAnswer.CustomSound(it))
                                },
                                modifier = Modifier.weight(1f),
                            )
                            AnimatedVisibility(isPendingAnswerValid != null) {
                                RequirementIcon(
                                    isFulfilled = isPendingAnswerValid ?: false,
                                    fulfilledTooltip = "Sound file path valid",
                                    notFulfilledTooltip = pendingAnswerInvalidReason ?: "Invalid",
                                )
                            }
                            RiftFileChooserButton(
                                typesDescription = "WAV audio files",
                                extensions = listOf("wav"),
                                onFileChosen = {
                                    text = it.absolutePathString()
                                    onFormAnswer(SoundAnswer.CustomSound(it.absolutePathString()))
                                },
                            )
                            RiftImageButton(
                                Res.drawable.play,
                                size = 16.dp,
                                onClick = { soundPlayer.playFile(text) },
                                modifier = Modifier.padding(horizontal = Spacing.small),
                            )
                        }
                        Text(
                            text = "You can test your sound with the play button.",
                            style = RiftTheme.typography.bodyPrimary,
                            modifier = Modifier.padding(vertical = Spacing.medium),
                        )
                    }
                }
            }

            is FormQuestion.SpecificCharactersQuestion -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 36.dp) // For requirement icon
                        .fillMaxWidth(),
                ) {
                    var text: String by remember { mutableStateOf("") }
                    val placeholder = if (formQuestion.allowEmpty) {
                        "Comma separated character names, or leave empty"
                    } else {
                        "Comma separated character names"
                    }
                    if (formQuestion.allowEmpty) {
                        LaunchedEffect(Unit) {
                            onFormAnswer(SpecificCharactersAnswer(emptyList()))
                        }
                    }
                    RiftTextField(
                        text = text,
                        placeholder = placeholder,
                        onTextChanged = {
                            text = it
                            val splitCharacters = text
                                .split(",")
                                .map { it.trim() }
                                .filterNot { it.isBlank() }
                            onFormAnswer(SpecificCharactersAnswer(splitCharacters))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isPendingAnswerValid != null) {
                        RequirementIcon(
                            isFulfilled = isPendingAnswerValid,
                            fulfilledTooltip = "Character names valid",
                            notFulfilledTooltip = pendingAnswerInvalidReason ?: "Invalid character names",
                            modifier = Modifier.padding(start = Spacing.medium),
                        )
                    }
                }
            }

            is FormQuestion.CombatTargetQuestion -> {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        var text: String by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            onFormAnswer(FreeformTextAnswer(""))
                        }
                        RiftTextField(
                            text = text,
                            placeholder = formQuestion.placeholder,
                            onTextChanged = {
                                text = it
                                onFormAnswer(FreeformTextAnswer(it.trim()))
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (recentTargets.isNotEmpty()) {
                            Spacer(Modifier.width(Spacing.medium))
                            RiftDropdown(
                                items = recentTargets.toList(),
                                selectedItem = "Recent targets",
                                onItemSelected = {
                                    text = it
                                    onFormAnswer(FreeformTextAnswer(it.trim()))
                                },
                                getItemName = {
                                    if (it.length > 20) it.take(20) + "…" else it
                                },
                            )
                        }
                    }
                    val helpText = if (recentTargets.isNotEmpty()) {
                        "You can choose from your recent targets above."
                    } else {
                        "Attack some targets in-game to get suggestions to what you can type above."
                    }
                    Text(
                        text = helpText,
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )
                }
            }

            is FormQuestion.PlanetaryIndustryColoniesQuestion -> {
                AnimatedContent(colonies.isNotEmpty()) { isNotEmpty ->
                    Column {
                        if (isNotEmpty) {
                            Text(
                                text = "Leave empty for any.",
                                style = RiftTheme.typography.bodySecondary,
                                modifier = Modifier.padding(bottom = Spacing.small),
                            )
                            LaunchedEffect(Unit) {
                                onFormAnswer(PlanetaryIndustryColoniesAnswer(emptyList()))
                            }
                            ScrollbarColumn(
                                modifier = Modifier.heightIn(max = 200.dp),
                            ) {
                                var selected: List<String> by remember { mutableStateOf(emptyList()) }
                                for (item in colonies) {
                                    val id = item.colony.id
                                    ListSelectorRow(
                                        text = item.colony.planet.name,
                                        description = item.characterName,
                                        isMultipleChoice = true,
                                        isSelected = id in selected,
                                        onSelect = {
                                            if (item.colony.id in selected) selected -= id else selected += id
                                            onFormAnswer(PlanetaryIndustryColoniesAnswer(selected))
                                        },
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No colonies available.\nCheck the Planetary Industry window.",
                                style = RiftTheme.typography.titlePrimary,
                            )
                        }
                    }
                }
            }

            is FormQuestion.FreeformTextQuestion -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    var text: String by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        onFormAnswer(FreeformTextAnswer(""))
                    }
                    RiftTextField(
                        text = text,
                        placeholder = formQuestion.placeholder,
                        onTextChanged = {
                            text = it
                            onFormAnswer(FreeformTextAnswer(it.trim()))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListSelectorRow(
    text: String,
    description: String?,
    isMultipleChoice: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    rightContent: @Composable () -> Unit = {},
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxWidth()
            .hoverBackground(pointerInteractionStateHolder = pointerInteractionStateHolder)
            .onClick { onSelect() }
            .padding(Spacing.medium),
    ) {
        if (isMultipleChoice) {
            RiftCheckbox(
                isChecked = isSelected,
                onCheckedChange = { onSelect() },
                pointerInteractionStateHolder = pointerInteractionStateHolder,
            )
        } else {
            RiftRadioButton(
                isChecked = isSelected,
                onChecked = onSelect,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            val style = if (isSelected) {
                RiftTheme.typography.titlePrimary.copy(color = RiftTheme.colors.primary)
            } else {
                RiftTheme.typography.titlePrimary
            }
            Text(
                text = text,
                style = style,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
        rightContent()
    }
}

private fun Pair<FormQuestion, FormAnswer>.toAnswerString(
    characters: List<LocalCharacter>,
): String? {
    val question = first
    val answer = second
    return when (question) {
        is FormQuestion.SingleChoiceQuestion -> {
            val id = (answer as SingleChoiceAnswer).id
            question.items.first { it.id == id }.text
        }

        is FormQuestion.MultipleChoiceQuestion -> {
            val ids = (answer as MultipleChoiceAnswer).ids
            question.items.filter { it.id in ids }.joinToString { it.text }
        }

        is FormQuestion.SystemQuestion -> {
            (answer as SystemAnswer).system.takeIf { it.isNotEmpty() }
        }

        is FormQuestion.JumpsRangeQuestion -> {
            val (min, max) = (answer as JumpsRangeAnswer).let { it.minJumps to it.maxJumps }
            val plural = if (max > 1) "s" else ""
            if (min == 0 && max == 0) {
                "Same system only"
            } else if (min == 0) {
                "Up to $max jump$plural away"
            } else if (min == max) {
                "Exactly $max jump$plural away"
            } else {
                "Between $min–$max jump$plural away"
            }
        }

        is FormQuestion.OwnedCharacterQuestion -> {
            val characterId = (answer as CharacterAnswer).characterId
            characters.firstOrNull { it.characterId == characterId }?.info?.success?.name ?: "$characterId"
        }

        is FormQuestion.IntelChannelQuestion -> {
            (answer as IntelChannelAnswer).channel
        }

        is FormQuestion.SoundQuestion -> {
            when (val soundAnswer = answer as SoundAnswer) {
                is SoundAnswer.BuiltInSound -> soundAnswer.item.name
                is SoundAnswer.CustomSound -> Path.of(soundAnswer.path).nameWithoutExtension
            }
        }

        is FormQuestion.SpecificCharactersQuestion -> {
            val specificCharacters = (answer as SpecificCharactersAnswer).characters
            when {
                specificCharacters.isEmpty() -> null
                specificCharacters.size == 1 -> specificCharacters.single()
                else -> "${specificCharacters.size} specific characters"
            }
        }

        is FormQuestion.CombatTargetQuestion -> {
            (answer as FreeformTextAnswer).text.takeIf { it.isNotBlank() }
        }

        is FormQuestion.PlanetaryIndustryColoniesQuestion -> {
            val colonies = (answer as PlanetaryIndustryColoniesAnswer).colonies
            when {
                colonies.isEmpty() -> "Any colony"
                colonies.size == 1 -> "A specific colony"
                else -> "${colonies.size} Specific colonies"
            }
        }

        is FormQuestion.FreeformTextQuestion -> {
            (answer as FreeformTextAnswer).text.takeIf { it.isNotBlank() }
        }
    }
}
