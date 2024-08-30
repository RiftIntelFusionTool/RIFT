package dev.nohus.rift.network.killboard

import dev.nohus.rift.killboard.KillmailConverter
import dev.nohus.rift.killboard.KillmailProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class KillboardObserver(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val killmailConverter: KillmailConverter,
    private val killmailProcessor: KillmailProcessor,
) {

    data class KillboardState(
        var socket: WebSocket? = null,
        var isConnected: Boolean = false,
        var connectionAttempts: Int = 0,
    )
    private val zKillboardState = KillboardState()
    private val eveKillState = KillboardState()
    val isZkillboardConnected: Boolean get() = zKillboardState.isConnected
    val isEveKillConnected: Boolean get() = eveKillState.isConnected

    suspend fun start() = coroutineScope {
        launch {
            maintainConnection(zKillboardState) {
                connect(
                    state = zKillboardState,
                    onMessage = ::onZkillboardMessage,
                    url = "wss://zkillboard.com/websocket/",
                    message = """{"action":"sub","channel":"killstream"}""",
                    name = "zKillboard",
                )
            }
        }
    }

    private suspend fun maintainConnection(state: KillboardState, connect: () -> Unit) {
        while (true) {
            if (!state.isConnected) {
                state.connectionAttempts++
                connect()
                delay(state.connectionAttempts.coerceAtMost(30) * 1_000L)
            } else {
                state.connectionAttempts = 0
                delay(5.seconds)
            }
        }
    }

    private fun connect(
        state: KillboardState,
        onMessage: (String) -> Unit,
        url: String,
        message: String,
        name: String,
    ) {
        state.socket?.cancel()
        val listener = KillboardWebSocketListener(onMessage, onConnection = { isConnected ->
            if (state.isConnected != isConnected) {
                state.isConnected = isConnected
                if (isConnected) logger.info { "$name connected" } else logger.warn { "$name disconnected" }
            }
        })
        val request = Request.Builder().url(url).build()
        state.socket = okHttpClient.newWebSocket(request, listener).apply {
            send(message)
        }
    }

    private fun onZkillboardMessage(body: String) {
        try {
            val message: ZkillboardKillmail = json.decodeFromString(body)
            val killmail = killmailConverter.convert(message)
            killmailProcessor.submit(killmail)
        } catch (e: IllegalArgumentException) {
            logger.error { "Invalid zKillboard message: $e" }
        }
    }

    private fun onEveKillMessage(body: String) {
        try {
            val message: EveKillKillmail = json.decodeFromString(body)
            val killmail = killmailConverter.convert(message)
            if (killmail != null) {
                killmailProcessor.submit(killmail)
            } else {
                logger.error { "Invalid EVE-KILL killmail: $message" }
            }
        } catch (e: IllegalArgumentException) {
            logger.error { "Invalid EVE-KILL message: $e" }
        }
    }
}
