package dev.nohus.rift.alerts.create

import dev.nohus.rift.alerts.create.FormQuestion.FreeformTextQuestion
import dev.nohus.rift.alerts.create.FormQuestion.IntelChannelQuestion
import dev.nohus.rift.alerts.create.FormQuestion.JumpsRangeQuestion
import dev.nohus.rift.alerts.create.FormQuestion.MultipleChoiceQuestion
import dev.nohus.rift.alerts.create.FormQuestion.OwnedCharacterQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SingleChoiceQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SoundQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SpecificCharactersQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SystemQuestion
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.repositories.ShipTypesRepository

@Suppress("PropertyName")
class CreateAlertQuestions(
    shipTypesRepository: ShipTypesRepository,
    configurationPackRepository: ConfigurationPackRepository,
) {
    private var id = 0

    // Alert trigger
    val ALERT_TRIGGER_INTEL_REPORTED = FormChoiceItem(id = id++, text = "Intel is reported")
    val ALERT_TRIGGER_GAME_ACTION = FormChoiceItem(id = id++, text = "Something happens in-game")
    val ALERT_TRIGGER_CHAT_MESSAGE = FormChoiceItem(id = id++, text = "Chat message is received")
    val ALERT_TRIGGER_JABBER_PING = FormChoiceItem(id = id++, text = "Jabber ping is received")
    val ALERT_TRIGGER_JABBER_MESSAGE = FormChoiceItem(id = id++, text = "Jabber message is received")
    val ALERT_TRIGGER_NO_MESSAGE = FormChoiceItem(id = id++, text = "No reports are being received")
    val ALERT_TRIGGER_QUESTION = SingleChoiceQuestion(
        title = "Trigger the alert when:",
        items = buildList {
            add(ALERT_TRIGGER_INTEL_REPORTED)
            add(ALERT_TRIGGER_GAME_ACTION)
            add(ALERT_TRIGGER_CHAT_MESSAGE)
            if (configurationPackRepository.isJabberEnabled()) {
                add(ALERT_TRIGGER_JABBER_PING)
                add(ALERT_TRIGGER_JABBER_MESSAGE)
            }
            add(ALERT_TRIGGER_NO_MESSAGE)
        },
    )

    // Intel report type
    val INTEL_REPORT_TYPE_ANY_CHARACTER = FormChoiceItem(id = id++, text = "Characters")
    val INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS = FormChoiceItem(id = id++, text = "Specific characters")
    val INTEL_REPORT_TYPE_ANY_SHIP = FormChoiceItem(id = id++, text = "Ships")
    val INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES = FormChoiceItem(id = id++, text = "Specific ship classes")
    val INTEL_REPORT_TYPE_WORMHOLE = FormChoiceItem(id = id++, text = "Wormholes")
    val INTEL_REPORT_TYPE_GATE_CAMP = FormChoiceItem(id = id++, text = "Gate camps")
    val INTEL_REPORT_TYPE_BUBBLES = FormChoiceItem(id = id++, text = "Bubbles")
    val INTEL_REPORT_TYPE_QUESTION = MultipleChoiceQuestion(
        title = "If the report contains any of:",
        items = listOf(
            INTEL_REPORT_TYPE_ANY_CHARACTER,
            INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS,
            INTEL_REPORT_TYPE_ANY_SHIP,
            INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES,
            INTEL_REPORT_TYPE_WORMHOLE,
            INTEL_REPORT_TYPE_GATE_CAMP,
            INTEL_REPORT_TYPE_BUBBLES,
        ),
    )

    // Intel report type, specific characters
    val INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS_QUESTION = SpecificCharactersQuestion(
        title = "And the characters reported include any of:",
        allowEmpty = false,
    )

    // Intel report type, specific ship classes
    val INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES_QUESTION = MultipleChoiceQuestion(
        title = "And the ships reported include any of:",
        items = shipTypesRepository.getShipClasses()
            .sorted()
            .mapIndexed { index, shipClass -> FormChoiceItem(index, shipClass) },
    )

    // Intel report location
    val INTEL_REPORT_LOCATION_SYSTEM = FormChoiceItem(id = id++, text = "A chosen system")
    val INTEL_REPORT_LOCATION_ANY_OWNED_CHARACTER =
        FormChoiceItem(id = id++, text = "Any online character's location")
    val INTEL_REPORT_LOCATION_OWNED_CHARACTER =
        FormChoiceItem(id = id++, text = "An online character's location")
    val INTEL_REPORT_LOCATION_QUESTION = SingleChoiceQuestion(
        title = "And it's reported near:",
        items = listOf(
            INTEL_REPORT_LOCATION_SYSTEM,
            INTEL_REPORT_LOCATION_ANY_OWNED_CHARACTER,
            INTEL_REPORT_LOCATION_OWNED_CHARACTER,
        ),
    )

    // Intel report location, system
    val INTEL_REPORT_LOCATION_SYSTEM_QUESTION = SystemQuestion(
        title = "With system name:",
        allowEmpty = false,
    )

    // Intel report location, specific character
    val INTEL_REPORT_LOCATION_OWNED_CHARACTER_QUESTION = OwnedCharacterQuestion(
        title = "Using character:",
    )

    // Intel report location, jumps range
    val INTEL_REPORT_LOCATION_JUMPS_RANGE_QUESTION = JumpsRangeQuestion(
        title = "At distance:",
    )

    // Game action type
    val GAME_ACTION_TYPE_IN_COMBAT = FormChoiceItem(
        id = id++,
        text = "You are in combat",
        description = "Includes both being under attack and attacking",
    )
    val GAME_ACTION_TYPE_UNDER_ATTACK = FormChoiceItem(
        id = id++,
        text = "You are under attack",
        description = "Includes being warp scrambled or energy neutralized",
    )
    val GAME_ACTION_TYPE_ATTACKING = FormChoiceItem(
        id = id++,
        text = "You are attacking",
    )
    val GAME_ACTION_TYPE_BEING_WARP_SCRAMBLED = FormChoiceItem(
        id = id++,
        text = "You are being warp scrambled",
    )
    val GAME_ACTION_TYPE_DECLOAKED = FormChoiceItem(
        id = id++,
        text = "You have been decloaked by a nearby object",
    )
    val GAME_ACTION_TYPE_QUESTION = MultipleChoiceQuestion(
        title = "If any of the following happens:",
        items = listOf(
            GAME_ACTION_TYPE_IN_COMBAT,
            GAME_ACTION_TYPE_UNDER_ATTACK,
            GAME_ACTION_TYPE_ATTACKING,
            GAME_ACTION_TYPE_BEING_WARP_SCRAMBLED,
            GAME_ACTION_TYPE_DECLOAKED,
        ),
    )

    // Game action type, combat target
    val GAME_ACTION_TYPE_COMBAT_TARGET_QUESTION = FreeformTextQuestion(
        title = "And the combat target name contains:",
        placeholder = "Dark Blood",
        allowEmpty = true,
    )

    // Game action type, decloak exceptions
    val GAME_ACTION_TYPE_DECLOAKED_EXCEPTIONS_QUESTION = FreeformTextQuestion(
        title = "Ignore being decloaked by objects containing:",
        placeholder = "Comma-separated list of keywords, e.g. gate",
        allowEmpty = true,
    )

    // Chat message, channel type
    val CHAT_MESSAGE_CHANNEL_ANY = FormChoiceItem(id = id++, text = "Any channel")
    val CHAT_MESSAGE_CHANNEL_SPECIFIC = FormChoiceItem(id = id++, text = "A chosen channel")
    val CHAT_MESSAGE_CHANNEL_TYPE_QUESTION = SingleChoiceQuestion(
        title = "In:",
        items = listOf(
            CHAT_MESSAGE_CHANNEL_ANY,
            CHAT_MESSAGE_CHANNEL_SPECIFIC,
        ),
    )

    // Chat message, specific channel
    val CHAT_MESSAGE_SPECIFIC_CHANNEL_QUESTION = FreeformTextQuestion(
        title = "With name:",
        placeholder = "Channel name",
        allowEmpty = false,
    )

    // Chat message, sender
    val CHAT_MESSAGE_SENDER_QUESTION = FreeformTextQuestion(
        title = "And the sender is:",
        placeholder = "Character name. Leave empty for any.",
        allowEmpty = true,
    )

    // Chat message, message contains
    val CHAT_MESSAGE_MESSAGE_CONTAINING_QUESTION = FreeformTextQuestion(
        title = "And the message contains:",
        placeholder = "Message contents. Leave empty for any.",
        allowEmpty = true,
    )

    // Jabber ping, ping type
    val JABBER_PING_TYPE_FLEET = FormChoiceItem(id = id++, text = "Fleet ping")
    val JABBER_PING_TYPE_MESSAGE = FormChoiceItem(id = id++, text = "Message ping")
    val JABBER_PING_TYPE_QUESTION = SingleChoiceQuestion(
        title = "And it's a:",
        items = listOf(
            JABBER_PING_TYPE_FLEET,
            JABBER_PING_TYPE_MESSAGE,
        ),
    )

    // Jabber ping, fleet ping, fleet commander
    val JABBER_PING_FLEET_COMMANDER_QUESTION = SpecificCharactersQuestion(
        title = "And the fleet commander is any of:",
        allowEmpty = true,
    )

    // Jabber ping, fleet ping, formup system
    val JABBER_PING_FLEET_FORMUP_SYSTEM_QUESTION = SystemQuestion(
        title = "And the fleet is forming up in system:",
        allowEmpty = true,
    )

    // Jabber ping, fleet ping, PAP type
    val JABBER_PING_FLEET_PAP_TYPE_STRATEGIC = FormChoiceItem(id = id++, text = "Strategic")
    val JABBER_PING_FLEET_PAP_TYPE_PEACETIME = FormChoiceItem(id = id++, text = "Peacetime")
    val JABBER_PING_FLEET_PAP_TYPE_ANY = FormChoiceItem(id = id++, text = "Any")
    val JABBER_PING_FLEET_PAP_TYPE_QUESTION = SingleChoiceQuestion(
        title = "And the PAP is:",
        items = listOf(
            JABBER_PING_FLEET_PAP_TYPE_STRATEGIC,
            JABBER_PING_FLEET_PAP_TYPE_PEACETIME,
            JABBER_PING_FLEET_PAP_TYPE_ANY,
        ),
    )

    // Jabber ping, fleet ping, doctrine
    val JABBER_PING_FLEET_DOCTRINE_QUESTION = FreeformTextQuestion(
        title = "And the doctrine contains:",
        placeholder = "Leave empty for any.",
        allowEmpty = true,
    )

    // Jabber ping, target
    val JABBER_PING_TARGET_QUESTION = FreeformTextQuestion(
        title = "And the target is:",
        placeholder = "E.g. \"beehive\". Leave empty for any.",
        allowEmpty = true,
    )

    // Jabber message, channel type
    val JABBER_MESSAGE_CHANNEL_ANY = FormChoiceItem(id = id++, text = "Any chat")
    val JABBER_MESSAGE_CHANNEL_SPECIFIC = FormChoiceItem(id = id++, text = "A chosen chat")
    val JABBER_MESSAGE_CHANNEL_DIRECT_MESSAGE = FormChoiceItem(id = id++, text = "A direct message")
    val JABBER_MESSAGE_CHANNEL_TYPE_QUESTION = SingleChoiceQuestion(
        title = "In:",
        items = listOf(
            JABBER_MESSAGE_CHANNEL_ANY,
            JABBER_MESSAGE_CHANNEL_SPECIFIC,
            JABBER_MESSAGE_CHANNEL_DIRECT_MESSAGE,
        ),
    )

    // Jabber message, specific channel
    val JABBER_MESSAGE_SPECIFIC_CHANNEL_QUESTION = FreeformTextQuestion(
        title = "With name:",
        placeholder = "Chat name (user or room)",
        allowEmpty = false,
    )

    // Jabber message, sender
    val JABBER_MESSAGE_SENDER_QUESTION = FreeformTextQuestion(
        title = "And the sender is:",
        placeholder = "User name. Leave empty for any.",
        allowEmpty = true,
    )

    // Jabber message, message contains
    val JABBER_MESSAGE_MESSAGE_CONTAINING_QUESTION = FreeformTextQuestion(
        title = "And the message contains:",
        placeholder = "Message contents. Leave empty for any.",
        allowEmpty = true,
    )

    // No message channel type
    val NO_MESSAGE_CHANNEL_ALL = FormChoiceItem(id = id++, text = "All monitored channels")
    val NO_MESSAGE_CHANNEL_ANY = FormChoiceItem(id = id++, text = "Any monitored channel")
    val NO_MESSAGE_CHANNEL_SPECIFIC = FormChoiceItem(id = id++, text = "A chosen channel")
    val NO_MESSAGE_CHANNEL_TYPE_QUESTION = SingleChoiceQuestion(
        title = "In:",
        items = listOf(
            NO_MESSAGE_CHANNEL_ALL,
            NO_MESSAGE_CHANNEL_ANY,
            NO_MESSAGE_CHANNEL_SPECIFIC,
        ),
    )

    // No message channel type, specific channel
    val NO_MESSAGE_CHANNEL_SPECIFIC_QUESTION = IntelChannelQuestion(
        title = "With name:",
    )

    // No message duration
    val NO_MESSAGE_DURATION_2_MINUTES = FormChoiceItem(id = id++, text = "2 minutes")
    val NO_MESSAGE_DURATION_5_MINUTES = FormChoiceItem(id = id++, text = "5 minutes")
    val NO_MESSAGE_DURATION_10_MINUTES = FormChoiceItem(id = id++, text = "10 minutes")
    val NO_MESSAGE_DURATION_20_MINUTES = FormChoiceItem(id = id++, text = "20 minutes")
    val NO_MESSAGE_DURATION_30_MINUTES = FormChoiceItem(id = id++, text = "30 minutes")
    val NO_MESSAGE_DURATION_QUESTION = SingleChoiceQuestion(
        title = "For at least:",
        items = listOf(
            NO_MESSAGE_DURATION_2_MINUTES,
            NO_MESSAGE_DURATION_5_MINUTES,
            NO_MESSAGE_DURATION_10_MINUTES,
            NO_MESSAGE_DURATION_20_MINUTES,
            NO_MESSAGE_DURATION_30_MINUTES,
        ),
    )

    // Alert action
    val ALERT_ACTION_RIFT_NOTIFICATION = FormChoiceItem(id = id++, text = "Send RIFT notification")
    val ALERT_ACTION_SYSTEM_NOTIFICATION = FormChoiceItem(id = id++, text = "Send system notification")
    val ALERT_ACTION_PLAY_SOUND = FormChoiceItem(id = id++, text = "Play sound")
    val ALERT_ACTION_SHOW_PING = FormChoiceItem(id = id++, text = "Show the ping")
    val ALERT_ACTION_QUESTION = MultipleChoiceQuestion(
        title = "When this alert is triggered:",
        items = listOf(
            ALERT_ACTION_RIFT_NOTIFICATION,
            ALERT_ACTION_SYSTEM_NOTIFICATION,
            ALERT_ACTION_PLAY_SOUND,
        ),
    )

    // Alert action (Jabber ping version)
    val ALERT_ACTION_JABBER_PING_QUESTION = MultipleChoiceQuestion(
        title = "When this alert is triggered:",
        items = listOf(
            ALERT_ACTION_SHOW_PING,
            ALERT_ACTION_PLAY_SOUND,
        ),
    )

    // Alert action, sound
    val ALERT_ACTION_SOUND_QUESTION = SoundQuestion(
        title = "Use sound:",
    )

    // Alert cooldown
    val ALERT_COOLDOWN_NONE = FormChoiceItem(id = id++, text = "Trigger every time")
    val ALERT_COOLDOWN_30_SECONDS = FormChoiceItem(id = id++, text = "30 seconds")
    val ALERT_COOLDOWN_1_MINUTE = FormChoiceItem(id = id++, text = "1 minute")
    val ALERT_COOLDOWN_2_MINUTES = FormChoiceItem(id = id++, text = "2 minutes")
    val ALERT_COOLDOWN_5_MINUTES = FormChoiceItem(id = id++, text = "5 minutes")
    val ALERT_COOLDOWN_10_MINUTES = FormChoiceItem(id = id++, text = "10 minutes")
    val ALERT_COOLDOWN_QUESTION = SingleChoiceQuestion(
        title = "Don't trigger again for:",
        items = listOf(
            ALERT_COOLDOWN_NONE,
            ALERT_COOLDOWN_30_SECONDS,
            ALERT_COOLDOWN_1_MINUTE,
            ALERT_COOLDOWN_2_MINUTES,
            ALERT_COOLDOWN_5_MINUTES,
            ALERT_COOLDOWN_10_MINUTES,
        ),
    )
}
