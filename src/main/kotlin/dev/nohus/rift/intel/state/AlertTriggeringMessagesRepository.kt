package dev.nohus.rift.intel.state

import dev.nohus.rift.intel.ParsedChannelChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

/**
 * Records intel report messages that have triggered an alert
 */
@Single
class AlertTriggeringMessagesRepository {

    data class AlertTriggeringMessage(
        val message: ParsedChannelChatMessage,
        val alertTriggerTimestamp: Instant,
    )

    private val _messages = MutableStateFlow<List<AlertTriggeringMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val maxAge = Duration.ofSeconds(10)

    fun add(message: ParsedChannelChatMessage) {
        val now = Instant.now()
        val minTimestamp = now - maxAge
        val new = AlertTriggeringMessage(message, now)
        _messages.update { messages ->
            messages.filter { it.alertTriggerTimestamp >= minTimestamp } + new
        }
    }
}
