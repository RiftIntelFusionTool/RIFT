package dev.nohus.rift.pings

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.jabber.client.UserChatController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class PingsRepository(
    private val jabberClient: JabberClient,
    private val parsePingUseCase: ParsePingUseCase,
    private val alertsTriggerController: AlertsTriggerController,
) {

    private val _pings = MutableStateFlow<List<PingModel>>(emptyList())
    val pings = _pings.asStateFlow()

    private var lastReceivedTimestamp: Instant = Instant.EPOCH

    suspend fun start() = coroutineScope {
        launch {
            jabberClient.state.map {
                it.userChatMessages.entries.firstOrNull {
                    it.key.xmppAddressOfChatPartner.localpartOrNull?.toString() in listOf("directorbot")
                }?.value ?: emptyList()
            }.collect {
                val newMessages = it.reversed().takeWhile { it.timestamp > lastReceivedTimestamp }
                newMessages.maxOfOrNull { it.timestamp }?.let { lastReceivedTimestamp = it }
                newMessages.forEach { onNewPingMessage(it) }
            }
        }
    }

    private suspend fun onNewPingMessage(message: UserChatController.UserMessage) {
        val ping = parsePingUseCase(message.timestamp, message.text) ?: return
        _pings.update { (it + ping).sortedBy { it.timestamp } }
        alertsTriggerController.onNewJabberPing(ping)
    }
}
