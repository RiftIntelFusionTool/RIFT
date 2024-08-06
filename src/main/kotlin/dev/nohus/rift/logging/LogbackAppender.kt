package dev.nohus.rift.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import dev.nohus.rift.utils.GetOperatingSystemUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.createNewFile
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.IOException
import org.jetbrains.skiko.MainUIDispatcher
import kotlin.io.path.appendText
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

private const val MAX_DIAGNOSTICS_LINES = 20_000

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
    private val diagnosticsFile = appDirectories.getAppDataDirectory().resolve("diagnostics")
    private var diagnosticsLines: Int = 0
    private val diagnosticsMutex = Mutex()

    override fun start() {
        encoder = PatternLayoutEncoder().apply {
            pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} -%kvp- %msg%n"
            context = this@LogbackAppender.context
            start()
        }
        try {
            diagnosticsFile.deleteIfExists()
            diagnosticsFile.createNewFile()
        } catch (ignore: IOException) {}
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

        LoggingRepository.append(event)
        writeDiagnostics(formatted)
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

    private fun writeDiagnostics(formatted: String) {
        scope.launch(Dispatchers.IO) {
            diagnosticsMutex.withLock {
                try {
                    diagnosticsFile.appendText(formatted)
                    diagnosticsLines++
                    if (diagnosticsLines >= 2 * MAX_DIAGNOSTICS_LINES) {
                        diagnosticsLines = MAX_DIAGNOSTICS_LINES
                        diagnosticsFile.writeText(diagnosticsFile.readLines().takeLast(MAX_DIAGNOSTICS_LINES).joinToString("\n") + "\n")
                    }
                } catch (ignored: IOException) {}
            }
        }
    }
}
