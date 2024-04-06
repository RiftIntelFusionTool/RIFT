package dev.nohus.rift.alerts.create

import dev.nohus.rift.Event
import dev.nohus.rift.ViewModel
import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.alerts.AlertAction
import dev.nohus.rift.alerts.AlertTrigger
import dev.nohus.rift.alerts.AlertsRepository
import dev.nohus.rift.alerts.ChatMessageChannel
import dev.nohus.rift.alerts.GameActionType
import dev.nohus.rift.alerts.IntelChannel
import dev.nohus.rift.alerts.IntelReportLocation
import dev.nohus.rift.alerts.IntelReportType
import dev.nohus.rift.alerts.JabberMessageChannel
import dev.nohus.rift.alerts.JabberPingType
import dev.nohus.rift.alerts.JumpRange
import dev.nohus.rift.alerts.PapType
import dev.nohus.rift.alerts.create.FormAnswer.CharacterAnswer
import dev.nohus.rift.alerts.create.FormAnswer.FreeformTextAnswer
import dev.nohus.rift.alerts.create.FormAnswer.IntelChannelAnswer
import dev.nohus.rift.alerts.create.FormAnswer.JumpsRangeAnswer
import dev.nohus.rift.alerts.create.FormAnswer.MultipleChoiceAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SingleChoiceAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SoundAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SpecificCharactersAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SystemAnswer
import dev.nohus.rift.alerts.create.FormQuestion.FreeformTextQuestion
import dev.nohus.rift.alerts.create.FormQuestion.IntelChannelQuestion
import dev.nohus.rift.alerts.create.FormQuestion.JumpsRangeQuestion
import dev.nohus.rift.alerts.create.FormQuestion.MultipleChoiceQuestion
import dev.nohus.rift.alerts.create.FormQuestion.OwnedCharacterQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SingleChoiceQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SoundQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SpecificCharactersQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SystemQuestion
import dev.nohus.rift.characters.LocalCharactersRepository
import dev.nohus.rift.characters.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.logs.parse.CharacterNameValidator
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.sound.Sound
import dev.nohus.rift.utils.sound.SoundsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import java.util.UUID

@Suppress("PrivatePropertyName")
@Factory
class CreateAlertViewModel(
    @InjectedParam private val inputModel: CreateAlertInputModel,
    private val localCharactersRepository: LocalCharactersRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val settings: Settings,
    private val alertsRepository: AlertsRepository,
    soundsRepository: SoundsRepository,
    private val characterNameValidator: CharacterNameValidator,
    shipTypesRepository: ShipTypesRepository,
) : ViewModel() {

    data class UiState(
        val formQuestion: FormQuestion?,
        val pendingAnswer: FormAnswer? = null,
        val isPendingAnswerValid: Boolean? = null,
        val pendingAnswerInvalidReason: String? = null,
        val formAnswers: List<Pair<FormQuestion, FormAnswer>> = emptyList(),
        val characters: List<LocalCharacter> = emptyList(),
        val intelChannels: List<String> = emptyList(),
        val sounds: List<Sound> = emptyList(),
        val dismissEvent: Event? = null,
        val highlightQuestionEvent: Event? = null,
    )

    private val questions = CreateAlertQuestions(
        shipTypesRepository = shipTypesRepository,
    )
    private val answers = mutableListOf<Pair<FormQuestion, FormAnswer>>()

    private val _state = MutableStateFlow(
        UiState(
            formQuestion = getNextFormQuestion(),
            sounds = soundsRepository.getSounds(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            localCharactersRepository.characters.collect { items ->
                _state.update { it.copy(characters = items) }
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(intelChannels = settings.intelChannels.map { it.name }) }
            settings.updateFlow.collect {
                _state.update { it.copy(intelChannels = it.intelChannels) }
            }
        }
    }

    fun onFormPendingAnswer(answer: FormAnswer) {
        _state.update { it.copy(pendingAnswer = answer, isPendingAnswerValid = null, pendingAnswerInvalidReason = null) }

        if (answer is SystemAnswer) {
            val question = _state.value.formQuestion as? SystemQuestion ?: return
            val systemExists = solarSystemsRepository.getSystemId(answer.system) != null
            val isValid = (question.allowEmpty && answer.system.isEmpty()) || systemExists
            _state.update { it.copy(isPendingAnswerValid = isValid) }
        } else if (answer is SpecificCharactersAnswer) {
            val question = _state.value.formQuestion as? SpecificCharactersQuestion ?: return
            if (answer.characters.isEmpty()) {
                if (question.allowEmpty) {
                    _state.update { it.copy(isPendingAnswerValid = true) }
                } else {
                    _state.update { it.copy(isPendingAnswerValid = false, pendingAnswerInvalidReason = "No names provided") }
                }
            } else {
                val firstInvalidCharacter = answer.characters.firstOrNull { !characterNameValidator.isValid(it) }
                if (firstInvalidCharacter != null) {
                    val reason = characterNameValidator.getInvalidReason(firstInvalidCharacter)
                    val reasonFullText = "Name \"$firstInvalidCharacter\" invalid:\n$reason"
                    _state.update { it.copy(isPendingAnswerValid = false, pendingAnswerInvalidReason = reasonFullText) }
                } else {
                    _state.update { it.copy(isPendingAnswerValid = true) }
                }
            }
        } else if (answer is FreeformTextAnswer) {
            val question = _state.value.formQuestion as? FreeformTextQuestion ?: return
            val isValid = question.allowEmpty || answer.text.isNotBlank()
            _state.update { it.copy(isPendingAnswerValid = isValid) }
        }
    }

    fun onBackClick() {
        if (answers.isNotEmpty()) {
            answers.removeLast()
            _state.update { it.copy(formQuestion = getNextFormQuestion(), formAnswers = answers) }
        } else {
            _state.update { it.copy(dismissEvent = Event()) }
        }
        _state.update { it.copy(pendingAnswer = null, isPendingAnswerValid = null) }
    }

    fun onContinueClick() {
        val question = _state.value.formQuestion
        if (question != null) {
            val answer = _state.value.pendingAnswer
            if (answer != null && _state.value.isPendingAnswerValid != false) {
                answers += question to answer
                val nextFormQuestion = getNextFormQuestion()
                _state.update {
                    it.copy(
                        formQuestion = nextFormQuestion,
                        pendingAnswer = null,
                        isPendingAnswerValid = null,
                        formAnswers = answers,
                    )
                }
            } else {
                _state.update { it.copy(highlightQuestionEvent = Event()) }
            }
        } else { // Form done, create alert
            val alert = buildAlert() ?: throw IllegalStateException()
            alertsRepository.add(alert)
            _state.update { it.copy(dismissEvent = Event()) }
        }
    }

    private fun getNextFormQuestion(): FormQuestion? = with(questions) {
        if (inputModel == CreateAlertInputModel.New) {
            val alertTrigger = ALERT_TRIGGER_QUESTION.answer ?: return ALERT_TRIGGER_QUESTION
            when (alertTrigger.id) {
                ALERT_TRIGGER_INTEL_REPORTED.id -> {
                    INTEL_REPORT_TYPE_QUESTION.answer ?: return INTEL_REPORT_TYPE_QUESTION
                    if (INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS.id in INTEL_REPORT_TYPE_QUESTION.answer!!.ids) {
                        INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS_QUESTION.answer ?: return INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS_QUESTION
                    }
                    if (INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES.id in INTEL_REPORT_TYPE_QUESTION.answer!!.ids) {
                        INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES_QUESTION.answer ?: return INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES_QUESTION
                    }
                    val reportLocation = INTEL_REPORT_LOCATION_QUESTION.answer ?: return INTEL_REPORT_LOCATION_QUESTION
                    when (reportLocation.id) {
                        INTEL_REPORT_LOCATION_SYSTEM.id -> {
                            INTEL_REPORT_LOCATION_SYSTEM_QUESTION.answer ?: return INTEL_REPORT_LOCATION_SYSTEM_QUESTION
                        }
                        INTEL_REPORT_LOCATION_ANY_OWNED_CHARACTER.id -> {}
                        INTEL_REPORT_LOCATION_OWNED_CHARACTER.id -> {
                            INTEL_REPORT_LOCATION_OWNED_CHARACTER_QUESTION.answer
                                ?: return INTEL_REPORT_LOCATION_OWNED_CHARACTER_QUESTION
                        }
                    }
                    INTEL_REPORT_LOCATION_JUMPS_RANGE_QUESTION.answer
                        ?: return INTEL_REPORT_LOCATION_JUMPS_RANGE_QUESTION
                }

                ALERT_TRIGGER_GAME_ACTION.id -> {
                    GAME_ACTION_TYPE_QUESTION.answer ?: return GAME_ACTION_TYPE_QUESTION
                    if (listOf(
                            GAME_ACTION_TYPE_IN_COMBAT.id,
                            GAME_ACTION_TYPE_UNDER_ATTACK.id,
                            GAME_ACTION_TYPE_ATTACKING.id,
                        ).any { it in GAME_ACTION_TYPE_QUESTION.answer!!.ids }
                    ) {
                        GAME_ACTION_TYPE_COMBAT_TARGET_QUESTION.answer ?: return GAME_ACTION_TYPE_COMBAT_TARGET_QUESTION
                    }
                }

                ALERT_TRIGGER_CHAT_MESSAGE.id -> {
                    val channelType = CHAT_MESSAGE_CHANNEL_TYPE_QUESTION.answer ?: return CHAT_MESSAGE_CHANNEL_TYPE_QUESTION
                    when (channelType.id) {
                        CHAT_MESSAGE_CHANNEL_ANY.id -> {}
                        CHAT_MESSAGE_CHANNEL_SPECIFIC.id -> {
                            CHAT_MESSAGE_SPECIFIC_CHANNEL_QUESTION.answer ?: return CHAT_MESSAGE_SPECIFIC_CHANNEL_QUESTION
                        }
                    }
                    CHAT_MESSAGE_SENDER_QUESTION.answer ?: return CHAT_MESSAGE_SENDER_QUESTION
                    CHAT_MESSAGE_MESSAGE_CONTAINING_QUESTION.answer ?: return CHAT_MESSAGE_MESSAGE_CONTAINING_QUESTION
                }

                ALERT_TRIGGER_JABBER_PING.id -> {
                    val type = JABBER_PING_TYPE_QUESTION.answer ?: return JABBER_PING_TYPE_QUESTION
                    when (type.id) {
                        JABBER_PING_TYPE_FLEET.id -> {
                            JABBER_PING_FLEET_COMMANDER_QUESTION.answer ?: return JABBER_PING_FLEET_COMMANDER_QUESTION
                            JABBER_PING_FLEET_FORMUP_SYSTEM_QUESTION.answer ?: return JABBER_PING_FLEET_FORMUP_SYSTEM_QUESTION
                            JABBER_PING_FLEET_PAP_TYPE_QUESTION.answer ?: return JABBER_PING_FLEET_PAP_TYPE_QUESTION
                            JABBER_PING_FLEET_DOCTRINE_QUESTION.answer ?: return JABBER_PING_FLEET_DOCTRINE_QUESTION
                            JABBER_PING_TARGET_QUESTION.answer ?: return JABBER_PING_TARGET_QUESTION
                        }
                        JABBER_PING_TYPE_MESSAGE.id -> {
                            JABBER_PING_TARGET_QUESTION.answer ?: return JABBER_PING_TARGET_QUESTION
                        }
                    }
                }

                ALERT_TRIGGER_JABBER_MESSAGE.id -> {
                    val channelType = JABBER_MESSAGE_CHANNEL_TYPE_QUESTION.answer ?: return JABBER_MESSAGE_CHANNEL_TYPE_QUESTION
                    when (channelType.id) {
                        JABBER_MESSAGE_CHANNEL_ANY.id -> {}
                        JABBER_MESSAGE_CHANNEL_SPECIFIC.id -> {
                            JABBER_MESSAGE_SPECIFIC_CHANNEL_QUESTION.answer ?: return JABBER_MESSAGE_SPECIFIC_CHANNEL_QUESTION
                        }
                    }
                    JABBER_MESSAGE_SENDER_QUESTION.answer ?: return JABBER_MESSAGE_SENDER_QUESTION
                    JABBER_MESSAGE_MESSAGE_CONTAINING_QUESTION.answer ?: return JABBER_MESSAGE_MESSAGE_CONTAINING_QUESTION
                }

                ALERT_TRIGGER_NO_MESSAGE.id -> {
                    val channelType = NO_MESSAGE_CHANNEL_TYPE_QUESTION.answer ?: return NO_MESSAGE_CHANNEL_TYPE_QUESTION
                    when (channelType.id) {
                        NO_MESSAGE_CHANNEL_ALL.id -> {}
                        NO_MESSAGE_CHANNEL_ANY.id -> {}
                        NO_MESSAGE_CHANNEL_SPECIFIC.id -> {
                            NO_MESSAGE_CHANNEL_SPECIFIC_QUESTION.answer ?: return NO_MESSAGE_CHANNEL_SPECIFIC_QUESTION
                        }
                    }
                    NO_MESSAGE_DURATION_QUESTION.answer ?: return NO_MESSAGE_DURATION_QUESTION
                }
            }
        }
        if (inputModel == CreateAlertInputModel.New || inputModel is CreateAlertInputModel.EditAction) {
            val alertActionQuestion = when (ALERT_TRIGGER_QUESTION.answer?.id) {
                ALERT_TRIGGER_JABBER_PING.id -> ALERT_ACTION_JABBER_PING_QUESTION
                else -> ALERT_ACTION_QUESTION
            }
            val alertActions = alertActionQuestion.answer ?: return alertActionQuestion
            if (ALERT_ACTION_PLAY_SOUND.id in alertActions.ids) {
                ALERT_ACTION_SOUND_QUESTION.answer ?: return ALERT_ACTION_SOUND_QUESTION
            }
            ALERT_COOLDOWN_QUESTION.answer ?: return ALERT_COOLDOWN_QUESTION
        }
        return null
    }

    private fun buildAlert(): Alert? = with(questions) {
        val alertTrigger = if (inputModel == CreateAlertInputModel.New) {
            when (ALERT_TRIGGER_QUESTION.answer?.id ?: return null) {
                ALERT_TRIGGER_INTEL_REPORTED.id -> {
                    val reportTypes = INTEL_REPORT_TYPE_QUESTION.answer?.let {
                        it.ids.map { id ->
                            when (id) {
                                INTEL_REPORT_TYPE_ANY_CHARACTER.id -> IntelReportType.AnyCharacter
                                INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS.id -> {
                                    val answer = INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS_QUESTION.answer ?: return null
                                    IntelReportType.SpecificCharacters(answer.characters)
                                }
                                INTEL_REPORT_TYPE_ANY_SHIP.id -> IntelReportType.AnyShip
                                INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES.id -> {
                                    val answer = INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES_QUESTION.answer ?: return null
                                    IntelReportType.SpecificShipClasses(answer.items.map { it.text })
                                }
                                INTEL_REPORT_TYPE_WORMHOLE.id -> IntelReportType.Wormhole
                                INTEL_REPORT_TYPE_GATE_CAMP.id -> IntelReportType.GateCamp
                                INTEL_REPORT_TYPE_BUBBLES.id -> IntelReportType.Bubbles
                                else -> throw IllegalStateException()
                            }
                        }
                    } ?: return null
                    val jumpsRange = INTEL_REPORT_LOCATION_JUMPS_RANGE_QUESTION.answer
                        ?.let { JumpRange(min = it.minJumps, max = it.maxJumps) } ?: return null
                    val reportLocation = when (INTEL_REPORT_LOCATION_QUESTION.answer?.id ?: return null) {
                        INTEL_REPORT_LOCATION_SYSTEM.id -> {
                            INTEL_REPORT_LOCATION_SYSTEM_QUESTION.answer?.let {
                                IntelReportLocation.System(systemName = it.system, jumpsRange = jumpsRange)
                            } ?: return null
                        }

                        INTEL_REPORT_LOCATION_ANY_OWNED_CHARACTER.id -> IntelReportLocation.AnyOwnedCharacter(
                            jumpsRange = jumpsRange,
                        )

                        INTEL_REPORT_LOCATION_OWNED_CHARACTER.id -> {
                            INTEL_REPORT_LOCATION_OWNED_CHARACTER_QUESTION.answer?.let {
                                IntelReportLocation.OwnedCharacter(
                                    characterId = it.characterId,
                                    jumpsRange = jumpsRange,
                                )
                            } ?: return null
                        }

                        else -> throw IllegalStateException()
                    }
                    AlertTrigger.IntelReported(
                        reportTypes = reportTypes,
                        reportLocation = reportLocation,
                    )
                }

                ALERT_TRIGGER_GAME_ACTION.id -> {
                    val actionTypes = GAME_ACTION_TYPE_QUESTION.answer?.let {
                        it.ids.map { id ->
                            when (id) {
                                GAME_ACTION_TYPE_IN_COMBAT.id -> {
                                    val answer = GAME_ACTION_TYPE_COMBAT_TARGET_QUESTION.answer ?: return null
                                    GameActionType.InCombat(answer.text.takeIf { it.isNotBlank() })
                                }
                                GAME_ACTION_TYPE_UNDER_ATTACK.id -> {
                                    val answer = GAME_ACTION_TYPE_COMBAT_TARGET_QUESTION.answer ?: return null
                                    GameActionType.UnderAttack(answer.text.takeIf { it.isNotBlank() })
                                }
                                GAME_ACTION_TYPE_ATTACKING.id -> {
                                    val answer = GAME_ACTION_TYPE_COMBAT_TARGET_QUESTION.answer ?: return null
                                    GameActionType.Attacking(answer.text.takeIf { it.isNotBlank() })
                                }
                                GAME_ACTION_TYPE_BEING_WARP_SCRAMBLED.id -> {
                                    GameActionType.BeingWarpScrambled
                                }
                                GAME_ACTION_TYPE_DECLOAKED.id -> {
                                    GameActionType.Decloaked
                                }
                                else -> throw IllegalStateException()
                            }
                        }
                    } ?: return null
                    AlertTrigger.GameAction(
                        actionTypes = actionTypes,
                    )
                }

                ALERT_TRIGGER_CHAT_MESSAGE.id -> {
                    val channel = when (CHAT_MESSAGE_CHANNEL_TYPE_QUESTION.answer?.id ?: return null) {
                        CHAT_MESSAGE_CHANNEL_ANY.id -> ChatMessageChannel.Any
                        CHAT_MESSAGE_CHANNEL_SPECIFIC.id -> {
                            val name = CHAT_MESSAGE_SPECIFIC_CHANNEL_QUESTION.answer?.text ?: return null
                            ChatMessageChannel.Channel(name)
                        }
                        else -> throw IllegalStateException()
                    }
                    val sender = CHAT_MESSAGE_SENDER_QUESTION.answer?.text ?: return null
                    val messageContaining = CHAT_MESSAGE_MESSAGE_CONTAINING_QUESTION.answer?.text ?: return null
                    AlertTrigger.ChatMessage(
                        channel = channel,
                        sender = sender.takeIf { it.isNotBlank() },
                        messageContaining = messageContaining.takeIf { it.isNotBlank() },
                    )
                }

                ALERT_TRIGGER_JABBER_PING.id -> {
                    when (JABBER_PING_TYPE_QUESTION.answer?.id ?: return null) {
                        JABBER_PING_TYPE_FLEET.id -> {
                            val fleetCommanders = JABBER_PING_FLEET_COMMANDER_QUESTION.answer ?: return null
                            val formupSystem = JABBER_PING_FLEET_FORMUP_SYSTEM_QUESTION.answer ?: return null
                            val papType = when (JABBER_PING_FLEET_PAP_TYPE_QUESTION.answer?.id ?: return null) {
                                JABBER_PING_FLEET_PAP_TYPE_STRATEGIC.id -> PapType.Strategic
                                JABBER_PING_FLEET_PAP_TYPE_PEACETIME.id -> PapType.Peacetime
                                JABBER_PING_FLEET_PAP_TYPE_ANY.id -> PapType.Any
                                else -> throw IllegalStateException()
                            }
                            val doctrine = JABBER_PING_FLEET_DOCTRINE_QUESTION.answer ?: return null
                            val target = JABBER_PING_TARGET_QUESTION.answer ?: return null
                            AlertTrigger.JabberPing(
                                pingType = JabberPingType.Fleet(
                                    fleetCommanders = fleetCommanders.characters,
                                    formupSystem = formupSystem.system.takeIf { it.isNotEmpty() },
                                    papType = papType,
                                    doctrineContaining = doctrine.text.takeIf { it.isNotEmpty() },
                                    target = target.text.takeIf { it.isNotEmpty() },
                                ),
                            )
                        }
                        JABBER_PING_TYPE_MESSAGE.id -> {
                            val target = JABBER_PING_TARGET_QUESTION.answer ?: return null
                            AlertTrigger.JabberPing(
                                pingType = JabberPingType.Message2(
                                    target = target.text.takeIf { it.isNotEmpty() },
                                ),
                            )
                        }
                        else -> throw IllegalStateException()
                    }
                }

                ALERT_TRIGGER_JABBER_MESSAGE.id -> {
                    val channel = when (JABBER_MESSAGE_CHANNEL_TYPE_QUESTION.answer?.id ?: return null) {
                        JABBER_MESSAGE_CHANNEL_ANY.id -> JabberMessageChannel.Any
                        JABBER_MESSAGE_CHANNEL_SPECIFIC.id -> {
                            val name = JABBER_MESSAGE_SPECIFIC_CHANNEL_QUESTION.answer?.text ?: return null
                            JabberMessageChannel.Channel(name)
                        }
                        JABBER_MESSAGE_CHANNEL_DIRECT_MESSAGE.id -> JabberMessageChannel.DirectMessage
                        else -> throw IllegalStateException()
                    }
                    val sender = JABBER_MESSAGE_SENDER_QUESTION.answer?.text ?: return null
                    val messageContaining = JABBER_MESSAGE_MESSAGE_CONTAINING_QUESTION.answer?.text ?: return null
                    AlertTrigger.JabberMessage(
                        channel = channel,
                        sender = sender.takeIf { it.isNotBlank() },
                        messageContaining = messageContaining.takeIf { it.isNotBlank() },
                    )
                }

                ALERT_TRIGGER_NO_MESSAGE.id -> {
                    val intelChannel = when (NO_MESSAGE_CHANNEL_TYPE_QUESTION.answer?.id ?: return null) {
                        NO_MESSAGE_CHANNEL_ALL.id -> IntelChannel.All
                        NO_MESSAGE_CHANNEL_ANY.id -> IntelChannel.Any
                        NO_MESSAGE_CHANNEL_SPECIFIC.id -> {
                            IntelChannel.Channel(
                                name = NO_MESSAGE_CHANNEL_SPECIFIC_QUESTION.answer?.channel ?: return null,
                            )
                        }
                        else -> throw IllegalStateException()
                    }
                    val durationSeconds = when (NO_MESSAGE_DURATION_QUESTION.answer?.id ?: return null) {
                        NO_MESSAGE_DURATION_2_MINUTES.id -> 60 * 2
                        NO_MESSAGE_DURATION_5_MINUTES.id -> 60 * 5
                        NO_MESSAGE_DURATION_10_MINUTES.id -> 60 * 10
                        NO_MESSAGE_DURATION_20_MINUTES.id -> 60 * 20
                        NO_MESSAGE_DURATION_30_MINUTES.id -> 60 * 30
                        else -> throw IllegalStateException()
                    }
                    AlertTrigger.NoChannelActivity(
                        channel = intelChannel,
                        durationSeconds = durationSeconds,
                    )
                }

                else -> throw IllegalStateException()
            }
        } else {
            null
        }

        val alertActions =
            if (inputModel == CreateAlertInputModel.New || inputModel is CreateAlertInputModel.EditAction) {
                val alertActionQuestion = when (ALERT_TRIGGER_QUESTION.answer?.id) {
                    ALERT_TRIGGER_JABBER_PING.id -> ALERT_ACTION_JABBER_PING_QUESTION
                    else -> ALERT_ACTION_QUESTION
                }
                alertActionQuestion.answer?.let {
                    it.ids.map { id ->
                        when (id) {
                            ALERT_ACTION_RIFT_NOTIFICATION.id -> AlertAction.RiftNotification
                            ALERT_ACTION_SYSTEM_NOTIFICATION.id -> AlertAction.SystemNotification
                            ALERT_ACTION_PLAY_SOUND.id -> {
                                val answer = ALERT_ACTION_SOUND_QUESTION.answer ?: return null
                                when (answer) {
                                    is SoundAnswer.BuiltInSound -> AlertAction.Sound(answer.item.id)
                                    is SoundAnswer.CustomSound -> AlertAction.CustomSound(answer.path)
                                }
                            }
                            ALERT_ACTION_SHOW_PING.id -> AlertAction.ShowPing
                            else -> throw IllegalStateException()
                        }
                    }
                } ?: return null
            } else {
                null
            }

        val cooldownSeconds = when (ALERT_COOLDOWN_QUESTION.answer?.id) {
            ALERT_COOLDOWN_NONE.id -> 0
            ALERT_COOLDOWN_30_SECONDS.id -> 30
            ALERT_COOLDOWN_1_MINUTE.id -> 60 * 1
            ALERT_COOLDOWN_2_MINUTES.id -> 60 * 2
            ALERT_COOLDOWN_5_MINUTES.id -> 60 * 5
            ALERT_COOLDOWN_10_MINUTES.id -> 60 * 10
            null -> 0 // Not asked for this alert
            else -> throw IllegalStateException()
        }

        return when (inputModel) {
            CreateAlertInputModel.New -> {
                Alert(
                    id = UUID.randomUUID().toString(),
                    trigger = alertTrigger ?: return null,
                    actions = alertActions ?: return null,
                    isEnabled = true,
                    cooldownSeconds = cooldownSeconds,
                    group = null,
                )
            }
            is CreateAlertInputModel.EditAction -> {
                inputModel.alert.copy(
                    actions = alertActions ?: return null,
                    cooldownSeconds = cooldownSeconds,
                )
            }
        }
    }

    private val FormQuestion.formAnswer: FormAnswer? get() = answers.lastOrNull { it.first == this }?.second
    private val SingleChoiceQuestion.answer: SingleChoiceAnswer? get() = formAnswer as? SingleChoiceAnswer
    private val MultipleChoiceQuestion.answer: MultipleChoiceAnswer? get() = formAnswer as? MultipleChoiceAnswer
    private val SystemQuestion.answer: SystemAnswer? get() = formAnswer as? SystemAnswer
    private val JumpsRangeQuestion.answer: JumpsRangeAnswer? get() = formAnswer as? JumpsRangeAnswer
    private val OwnedCharacterQuestion.answer: CharacterAnswer? get() = formAnswer as? CharacterAnswer
    private val IntelChannelQuestion.answer: IntelChannelAnswer? get() = formAnswer as? IntelChannelAnswer
    private val SoundQuestion.answer: SoundAnswer? get() = formAnswer as? SoundAnswer
    private val SpecificCharactersQuestion.answer: SpecificCharactersAnswer? get() = formAnswer as? SpecificCharactersAnswer
    private val FreeformTextQuestion.answer: FreeformTextAnswer? get() = formAnswer as? FreeformTextAnswer
}
