package dev.nohus.rift.network.zkillboard

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class KillboardObserver(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val killmailProcessor: KillmailProcessor,
) {

    private var socket: WebSocket? = null
    private var isConnected = false

    suspend fun start() = coroutineScope {
        while (true) {
            if (!isConnected) {
                connect()
            }
            delay(1_000)
        }
    }

    private fun connect() {
        socket?.cancel()
        val listener = KillboardWebSocketListener(::onMessage, ::onConnection)
        socket = okHttpClient.newWebSocket(createRequest(), listener).apply {
            subscribe("killstream")
        }
    }

    private fun onMessage(body: String) {
        try {
            val message: KillboardMessage = json.decodeFromString(body)
            killmailProcessor.submit(message)
        } catch (e: IllegalArgumentException) {
            logger.error { "Invalid zKillboard message: $e" }
        }
    }

    private fun onConnection(state: Boolean) {
        if (isConnected != state) {
            isConnected = state
            if (state) {
                logger.info { "Killboard connected" }
            } else {
                logger.warn { "Killboard disconnected" }
            }
        }
    }

    private fun createRequest(): Request {
        val url = "wss://zkillboard.com/websocket/"
        return Request.Builder().url(url).build()
    }

    private fun WebSocket.subscribe(channel: String) {
        send("""{"action":"sub","channel":"$channel"}""")
    }
}
