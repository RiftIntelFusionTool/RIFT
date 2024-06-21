package dev.nohus.rift.alerts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import dev.nohus.rift.alerts.AlertsTriggerController.AlertLocationMatch
import dev.nohus.rift.gamelogs.GameLogAction
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.logging.analytics.Analytics
import dev.nohus.rift.logs.parse.ChannelChatMessage
import dev.nohus.rift.notifications.NotificationsController
import dev.nohus.rift.notifications.NotificationsController.Notification
import dev.nohus.rift.notifications.system.SendNotificationUseCase
import dev.nohus.rift.repositories.CharactersRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.utils.sound.SoundPlayer
import dev.nohus.rift.utils.sound.SoundsRepository
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.absolutePathString

@Single
class AlertsActionController(
    private val sendNotificationUseCase: SendNotificationUseCase,
    private val soundPlayer: SoundPlayer,
    private val soundsRepository: SoundsRepository,
    private val notificationsController: NotificationsController,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val charactersRepository: CharactersRepository,
    private val windowManager: WindowManager,
    private val analytics: Analytics,
) {

    private val scope = CoroutineScope(Job())
    private var lastSoundTimestamp = Instant.EPOCH

    fun triggerIntelAlert(
        alert: Alert,
        matchingEntities: List<Pair<IntelReportType, List<SystemEntity>>>,
        entities: List<SystemEntity>,
        locationMatch: AlertLocationMatch,
        solarSystem: String,
    ) {
        val title = getNotificationTitle(matchingEntities)
        val message = getNotificationMessage(locationMatch)
        val notification = Notification.IntelNotification(title, locationMatch, entities, solarSystem)
        triggerAlert(alert, notification, title, message)
    }

    fun triggerGameActionAlert(alert: Alert, action: GameLogAction, characterId: Int) {
        val title = getNotificationTitle(action)
        val message = getNotificationMessage(action)
        val typeId = getNotificationTypeId(action)
        val notification = Notification.TextNotification(title, message, characterId, typeId)
        triggerAlert(alert, notification, title, message.toString())
    }

    fun triggerChatMessageAlert(alert: Alert, chatMessage: ChannelChatMessage) {
        scope.launch {
            val characterId = charactersRepository.getCharacterId(chatMessage.chatMessage.author)
            val title = "Chat message in ${chatMessage.metadata.channelName}"
            val message = "${chatMessage.chatMessage.author}: ${chatMessage.chatMessage.message}"
            val notification = Notification.ChatMessageNotification(
                channel = chatMessage.metadata.channelName,
                message = chatMessage.chatMessage.message,
                sender = chatMessage.chatMessage.author,
                senderCharacterId = characterId,
            )
            triggerAlert(alert, notification, title, message)
        }
    }

    fun triggerJabberMessageAlert(alert: Alert, chat: String, sender: String, message: String) {
        scope.launch {
            val title = "Jabber message in $chat"
            val notification = Notification.JabberMessageNotification(
                chat = chat,
                message = message,
                sender = sender,
            )
            triggerAlert(alert, notification, title, message)
        }
    }

    fun triggerJabberPingAlert(alert: Alert) {
        // Ping alerts cannot generate notifications
        val notification = Notification.TextNotification("", AnnotatedString(""), null, null)
        triggerAlert(alert, notification, "", "")
    }

    @OptIn(ExperimentalTextApi::class)
    fun triggerInactiveChannelAlert(alert: Alert, triggeredInactiveChannels: List<String>) {
        val styleTag = Notification.TextNotification.styleTag
        val styleValue = Notification.TextNotification.styleValue
        val message = if (triggeredInactiveChannels.size == 1) {
            buildAnnotatedString {
                append("Channel ")
                withAnnotation(styleTag, styleValue) {
                    append(triggeredInactiveChannels.single())
                }
                append(" appears inactive")
            }
        } else {
            buildAnnotatedString {
                append("Channels ")
                withAnnotation(styleTag, styleValue) {
                    append(triggeredInactiveChannels.joinToString())
                }
                append(" appear inactive")
            }
        }
        val title = "Not receiving intel"
        val notification = Notification.TextNotification(title, message, null, null)
        triggerAlert(alert, notification, title, message.toString())
    }

    private fun triggerAlert(alert: Alert, notification: Notification, title: String, message: String) {
        analytics.alertTriggered()
        alert.actions.forEach { action ->
            when (action) {
                AlertAction.RiftNotification -> sendRiftNotification(notification)
                AlertAction.SystemNotification -> sendSystemNotification(title, message)
                is AlertAction.Sound -> {
                    withSoundCooldown {
                        val sound = soundsRepository.getSounds().firstOrNull { it.id == action.id } ?: return@withSoundCooldown
                        soundPlayer.play(sound.resource)
                    }
                }
                is AlertAction.CustomSound -> {
                    withSoundCooldown {
                        soundPlayer.playFile(action.path)
                    }
                }
                AlertAction.ShowPing -> windowManager.onWindowOpen(WindowManager.RiftWindow.Pings)
            }
        }
    }

    private fun withSoundCooldown(block: () -> Unit) {
        val duration = Duration.between(lastSoundTimestamp, Instant.now())
        if (duration >= Duration.ofMillis(200)) {
            lastSoundTimestamp = Instant.now()
            block()
        }
    }

    private fun getNotificationTitle(matchingEntities: List<Pair<IntelReportType, List<SystemEntity>>>): String {
        return matchingEntities.map { it.first }.let {
            if (IntelReportType.GateCamp in it) {
                "Gate camp reported"
            } else if (IntelReportType.AnyCharacter in it) {
                "Hostile reported"
            } else if (IntelReportType.AnyShip in it) {
                "Hostile ship reported"
            } else if (IntelReportType.Bubbles in it) {
                "Bubbles reported"
            } else if (IntelReportType.Wormhole in it) {
                "Wormhole reported"
            } else {
                "Intel alert"
            }
        }
    }

    /**
     * Only used for system notifications which cannot show rich content
     */
    private fun getNotificationMessage(locationMatch: AlertLocationMatch): String {
        val message = when (locationMatch) {
            is AlertLocationMatch.System -> {
                val distanceText = when (val distance = locationMatch.distance) {
                    0 -> "In system"
                    1 -> "1 jump away from"
                    else -> "$distance jumps away from"
                }
                val systemName = solarSystemsRepository.getSystemName(locationMatch.systemId) ?: "${locationMatch.systemId}"
                "$distanceText $systemName"
            }

            is AlertLocationMatch.Character -> {
                when (val distance = locationMatch.distance) {
                    0 -> "In your system"
                    1 -> "1 jump away"
                    else -> "$distance jumps away"
                }
            }
        }
        return message
    }

    private fun getNotificationTitle(action: GameLogAction): String {
        return when (action) {
            is GameLogAction.UnderAttack -> "Under attack"
            is GameLogAction.Attacking -> "Attacking"
            is GameLogAction.BeingWarpScrambled -> "Warp scrambled"
            is GameLogAction.Decloaked -> "Decloaked"
        }
    }

    @OptIn(ExperimentalTextApi::class)
    private fun getNotificationMessage(action: GameLogAction): AnnotatedString {
        val styleTag = Notification.TextNotification.styleTag
        val styleValue = Notification.TextNotification.styleValue
        return when (action) {
            is GameLogAction.UnderAttack -> buildAnnotatedString {
                append("Attacker is ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.Attacking -> buildAnnotatedString {
                append("Target is ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.BeingWarpScrambled -> buildAnnotatedString {
                append("Tackled by ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.Decloaked -> buildAnnotatedString {
                withAnnotation(styleTag, styleValue) {
                    append(action.by)
                }
                append(" is too close")
            }
        }
    }

    private fun getNotificationTypeId(action: GameLogAction): Int? {
        return when (action) {
            is GameLogAction.UnderAttack -> typesRepository.getTypeId(action.target)
            is GameLogAction.Attacking -> typesRepository.getTypeId(action.target)
            is GameLogAction.BeingWarpScrambled -> typesRepository.getTypeId(action.target)
            is GameLogAction.Decloaked -> typesRepository.getTypeId(action.by)
        }
    }

    private fun sendRiftNotification(notification: Notification) {
        notificationsController.show(notification)
    }

    private fun sendSystemNotification(title: String, message: String) {
        sendNotificationUseCase(
            appName = "RIFT Intel Fusion Tool",
            iconPath = Path.of("icon/icon-512.png").absolutePathString(),
            summary = title,
            body = message,
            timeout = 8,
        )
    }
}
