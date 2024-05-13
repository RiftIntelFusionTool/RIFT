package dev.nohus.rift.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import dev.nohus.rift.utils.GetOperatingSystemUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.osdirectories.LinuxDirectories
import dev.nohus.rift.utils.osdirectories.MacDirectories
import dev.nohus.rift.utils.osdirectories.WindowsDirectories
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.IOException
import org.jetbrains.skiko.MainUIDispatcher
import java.io.File

class LogbackAppender : AppenderBase<ILoggingEvent>() {

    private val scope = CoroutineScope(Job())
    private var encoder: PatternLayoutEncoder? = null

    // Creating these dependencies manually because DI is initialized after logging in case DI crashes
    private val appDirectories = AppDirectories(
        when (GetOperatingSystemUseCase()()) {
            OperatingSystem.Linux -> LinuxDirectories()
            OperatingSystem.Windows -> WindowsDirectories()
            OperatingSystem.MacOs -> MacDirectories()
        },
    )
    private val diagnosticsFile = File(appDirectories.getAppDataDirectory(), "diagnostics").also { it.delete() }

    override fun start() {
        encoder = PatternLayoutEncoder().apply {
            pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} -%kvp- %msg%n"
            context = this@LogbackAppender.context
            start()
        }
        super.start()
    }

    override fun stop() {
        encoder?.stop()
        super.stop()
    }

    override fun append(event: ILoggingEvent) {
        val formatted = encoder?.encode(event)?.let { String(it) } ?: event.message
        val breadcrumb = Breadcrumb().apply {
            category = event.loggerName
            message = anonymizeLogMessage(formatted)
            level = when (event.level) {
                Level.ERROR -> SentryLevel.ERROR
                Level.WARN -> SentryLevel.WARNING
                Level.INFO -> SentryLevel.INFO
                Level.DEBUG -> SentryLevel.DEBUG
                else -> return
            }
        }
        scope.launch(MainUIDispatcher) {
            Sentry.addBreadcrumb(breadcrumb)
        }
        scope.launch(Dispatchers.IO) {
            try {
                diagnosticsFile.appendText(formatted)
            } catch (ignored: IOException) {}
        }
    }

    private val anonymizationReplacements = listOf(
        """/characters/[0-9]{8,10}""".toRegex() to "/characters/000000000",
        """character [0-9]{8,10}""".toRegex() to "character 000000000",
        """/structures/[0-9]{12,14}/""".toRegex() to "/structures/0000000000000/",
        """/stations/[0-9]{8}/""".toRegex() to "/stations/00000000/",
        """/corporations/[0-9]{7,10}""".toRegex() to "/corporations/000000000",
        """/alliances/[0-9]{8,11}""".toRegex() to "/alliances/0000000000",
    )

    private fun anonymizeLogMessage(message: String): String {
        return anonymizationReplacements.fold(message) { acc, (from, to) ->
            acc.replace(from, to)
        }
    }
}
