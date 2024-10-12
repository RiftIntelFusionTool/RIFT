package dev.nohus.rift.crash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.awaitApplication
import dev.nohus.rift.compose.theme.RiftTheme
import io.sentry.Sentry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Window
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
object RiftExceptionHandlerFactory : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: Window) = WindowExceptionHandler { throwable ->
        handleFatalException(throwable)
    }
}

var isCrashed = AtomicBoolean(false)
fun handleFatalException(throwable: Throwable, window: Window? = null) {
    if (isCrashed.getAndSet(true)) return
    val sentryId = Sentry.captureException(throwable)
    println("RIFT has encountered a fatal issue: $sentryId")
    SwingUtilities.invokeLater {
        showErrorDialog(throwable, sentryId.toString())
        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun showErrorDialog(throwable: Throwable, errorId: String) {
    GlobalScope.launch {
        awaitApplication {
            RiftTheme {
                var isOpen by remember { mutableStateOf(true) }
                if (isOpen) {
                    RiftExceptionWindow(
                        throwable = throwable,
                        errorId = errorId,
                        onCloseRequest = {
                            isOpen = false
                            exitProcess(-1)
                        },
                    )
                }
            }
        }
    }
}
