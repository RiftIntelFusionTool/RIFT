package dev.nohus.rift.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.skiko.MainUIDispatcher

class LogbackAppender : AppenderBase<ILoggingEvent>() {

    private val scope = CoroutineScope(Job())

    override fun append(event: ILoggingEvent) {
        val breadcrumb = Breadcrumb().apply {
            category = event.loggerName
            message = anonymizeLogMessage(event.message)
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
    }

    private val anonymizationReplacements = listOf(
        """/characters/[0-9]{8,10}""".toRegex() to "/characters/000000000",
        """character [0-9]{8,10}""".toRegex() to "character 000000000",
        """/structures/[0-9]{12,14}/""".toRegex() to "/structures/0000000000000/",
        """/corporations/[0-9]{7,10}""".toRegex() to "/corporations/000000000",
        """/alliances/[0-9]{8,11}""".toRegex() to "/alliances/0000000000",
    )

    private fun anonymizeLogMessage(message: String): String {
        return anonymizationReplacements.fold(message) { acc, (from, to) ->
            acc.replace(from, to)
        }
    }
}
