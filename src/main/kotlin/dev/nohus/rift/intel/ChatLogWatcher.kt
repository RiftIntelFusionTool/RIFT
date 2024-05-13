package dev.nohus.rift.intel

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.location.LocalSystemChangeController
import dev.nohus.rift.logs.ChatLogsObserver
import dev.nohus.rift.logs.DetectLogsDirectoryUseCase
import dev.nohus.rift.logs.GetChatLogsDirectoryUseCase
import dev.nohus.rift.logs.parse.ChannelChatMessage
import dev.nohus.rift.logs.parse.ChatMessageParser
import dev.nohus.rift.logs.parse.ChooseChatMessageTokenizationUseCase
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class ChatLogWatcher(
    private val chatLogsObserver: ChatLogsObserver,
    private val detectLogsDirectoryUseCase: DetectLogsDirectoryUseCase,
    private val getChatLogsDirectoryUseCase: GetChatLogsDirectoryUseCase,
    private val settings: Settings,
    private val chatMessageParser: ChatMessageParser,
    private val chooseChatMessageTokenizationUseCase: ChooseChatMessageTokenizationUseCase,
    private val intelStateController: IntelStateController,
    private val localSystemChangeController: LocalSystemChangeController,
    private val alertsTriggerController: AlertsTriggerController,
) {

    private val _channelChatMessages = MutableStateFlow<List<ParsedChannelChatMessage>>(emptyList())
    val channelChatMessages = _channelChatMessages.asStateFlow()

    private var watchedDirectory: Path? = null
    private val mutexByChannel = mutableMapOf<String, Mutex>()

    suspend fun start() = coroutineScope {
        launch {
            observeChatLogs()
        }
        launch {
            settings.updateFlow.collect {
                settings.eveLogsDirectory.let { new ->
                    if (new != watchedDirectory) {
                        launch {
                            observeChatLogs()
                        }
                    }
                }
            }
        }
    }

    private suspend fun observeChatLogs() {
        chatLogsObserver.stop()
        val logsDirectory = settings.eveLogsDirectory ?: detectLogsDirectoryUseCase()
        val chatLogsDirectory = getChatLogsDirectoryUseCase(logsDirectory)
        if (chatLogsDirectory != null) {
            watchedDirectory = logsDirectory
            observeChatLogs(chatLogsDirectory)
        } else {
            logger.warn { "No chat logs directory detected" }
        }
    }

    private suspend fun observeChatLogs(directory: Path) = coroutineScope {
        chatLogsObserver.observe(directory) { channelChatMessage ->
            launch {
                getMutex(channelChatMessage.metadata.channelName).withLock {
                    val duration = Duration.between(channelChatMessage.chatMessage.timestamp, Instant.now())
                    if (duration < Duration.ofSeconds(10)) {
                        alertsTriggerController.onNewChatMessage(channelChatMessage)
                    }

                    if (isMessageRelevantForIntel(channelChatMessage)) {
                        val region = getIntelRegion(channelChatMessage)
                        if (region != null) {
                            try {
                                val parsings = chatMessageParser.parse(channelChatMessage.chatMessage.message, region)
                                if (parsings.isNotEmpty()) {
                                    val bestParsing = chooseChatMessageTokenizationUseCase(parsings)
                                    val parsed = ParsedChannelChatMessage(
                                        chatMessage = channelChatMessage.chatMessage,
                                        channelRegion = region,
                                        metadata = channelChatMessage.metadata,
                                        parsed = bestParsing,
                                    )

                                    val context = getMessageContext(parsed)
                                    intelStateController.submitMessage(parsed, context)
                                    _channelChatMessages.update { previous ->
                                        (previous + parsed).sortedBy { it.chatMessage.timestamp }
                                    }
                                }
                            } catch (e: Exception) {
                                Sentry.captureException(IOException("Could not parse: \"${channelChatMessage.chatMessage.message}\", region: \"$region\"", e))
                            }
                        }
                    }

                    if (isMessageLocalEveSystem(channelChatMessage)) {
                        if (channelChatMessage.chatMessage.message.startsWith("Channel changed to Local")) {
                            val system = channelChatMessage.chatMessage.message.substringAfter(" : ")
                            localSystemChangeController.onSystemChangeMessage(
                                system = system,
                                timestamp = channelChatMessage.chatMessage.timestamp,
                                characterId = channelChatMessage.metadata.characterId,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves recent chat messages preceding this chat message
     */
    private fun getMessageContext(parsed: ParsedChannelChatMessage): List<ParsedChannelChatMessage> {
        return _channelChatMessages.value.reversed()
            .asSequence()
            .filter { it.metadata.channelId == parsed.metadata.channelId }
            .take(10)
            .filter { Duration.between(it.chatMessage.timestamp, parsed.chatMessage.timestamp) < Duration.ofMinutes(5) }
            .toList()
    }

    private fun isMessageLocalEveSystem(message: ChannelChatMessage): Boolean {
        if (message.metadata.channelName != "Local") return false
        if (message.chatMessage.author != "EVE System") return false
        return true
    }

    @Suppress("RedundantIf")
    private fun isMessageRelevantForIntel(message: ChannelChatMessage): Boolean {
        if (!isIntelChannel(message.metadata.channelName)) return false
        if (message.chatMessage.author == "EVE System") return false
        val messageAge = Duration.between(message.chatMessage.timestamp, Instant.now())
        val isMessageOld = messageAge > Duration.ofMinutes(10)
        if (isMessageOld && !settings.isLoadOldMessagesEnabled) return false
        return true
    }

    private fun getMutex(channel: String): Mutex {
        return mutexByChannel[channel] ?: Mutex().also { mutexByChannel[channel] = it }
    }

    private fun isIntelChannel(channelName: String): Boolean {
        return channelName in settings.intelChannels.map { it.name }
    }

    private fun getIntelRegion(message: ChannelChatMessage): String? {
        return settings.intelChannels.firstOrNull { it.name == message.metadata.channelName }?.region
    }
}
