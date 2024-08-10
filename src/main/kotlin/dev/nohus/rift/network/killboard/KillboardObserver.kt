package dev.nohus.rift.network.killboard

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
    private val killmailConverter: KillmailConverter,
    private val killmailProcessor: KillmailProcessor,
) {

    private var zKillboardSocket: WebSocket? = null
    private var eveKillSocket: WebSocket? = null
    var isZkillboardConnected = false
        private set
    var isEveKillConnected = false
        private set

    suspend fun start() = coroutineScope {
        while (true) {
            if (!isZkillboardConnected || !isEveKillConnected) connect()
            delay(1_000)
        }
    }

    private fun connect() {
        if (!isZkillboardConnected) {
            zKillboardSocket?.cancel()
            val listener = KillboardWebSocketListener(::onZkillboardMessage, ::onZkillboardConnection)
            val request = Request.Builder().url("wss://zkillboard.com/websocket/").build()
            zKillboardSocket = okHttpClient.newWebSocket(request, listener).apply {
                send("""{"action":"sub","channel":"killstream"}""")
            }
        }
        if (!isEveKillConnected) {
            eveKillSocket?.cancel()
            val listener = KillboardWebSocketListener(::onEveKillMessage, ::onEveKillConnection)
            val request = Request.Builder().url("wss://ws.eve-kill.com/kills").build()
            eveKillSocket = okHttpClient.newWebSocket(request, listener).apply {
                send("""{"type":"subscribe","data":["all"]}""")
            }
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

    private fun onZkillboardConnection(state: Boolean) {
        if (isZkillboardConnected != state) {
            isZkillboardConnected = state
            if (state) logger.info { "zKillboard connected" } else logger.warn { "zKillboard disconnected" }
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

    private fun onEveKillConnection(state: Boolean) {
        if (isEveKillConnected != state) {
            isEveKillConnected = state
            if (state) logger.info { "EVE-KILL connected" } else logger.warn { "EVE-KILL disconnected" }
        }
    }
}
