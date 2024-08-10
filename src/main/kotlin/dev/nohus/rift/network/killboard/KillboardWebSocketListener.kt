package dev.nohus.rift.network.killboard

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class KillboardWebSocketListener(
    private val onMessage: (String) -> Unit,
    private val onConnection: (Boolean) -> Unit,
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        onConnection(true)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        onMessage(text)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        onConnection(false)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        onConnection(false)
    }
}
