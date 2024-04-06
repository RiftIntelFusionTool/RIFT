package dev.nohus.rift.sso.authentication

import dev.nohus.rift.generated.resources.Res
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

private val logger = KotlinLogging.logger {}

@Factory
class CallbackServer {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var server: NettyApplicationEngine? = null
    private var callback: (suspend (code: String, state: String) -> Unit)? = null

    fun start(port: Int) {
        stop()
        scope.launch {
            createCallbackServer(port).let {
                server = it
                logger.info { "Starting server" }
                it.start(wait = true)
            }
        }
    }

    fun setCallback(block: suspend (code: String, state: String) -> Unit) {
        callback = block
    }

    fun stop() = scope.launch {
        if (server != null) logger.info { "Stopping server" }
        server?.stop(0, 0)
        server = null
    }

    private suspend fun createCallbackServer(port: Int): NettyApplicationEngine {
        return embeddedServer(Netty, port = port) {
            routing {
                get("/callback") {
                    val code = call.parameters["code"] ?: return@get
                    val state = call.parameters["state"] ?: return@get
                    val html = Res.readBytes("files/sso-callback.html").inputStream().bufferedReader().readText()
                    call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
                    callback?.invoke(code, state)
                }
            }
        }
    }
}
