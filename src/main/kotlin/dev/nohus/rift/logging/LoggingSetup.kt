package dev.nohus.rift.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import dev.nohus.rift.BuildConfig
import org.jivesoftware.smack.util.PacketParserUtils
import org.slf4j.LoggerFactory

private val logger = java.util.logging.Logger.getLogger(PacketParserUtils::class.java.getName())

fun initializeLogging() {
    silenceChattyThirdPartyLoggers()
    @Suppress("KotlinConstantConditions")
    if (BuildConfig.environment == "dev") {
        val logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger

        val patternLayoutEncoder = PatternLayoutEncoder()
        patternLayoutEncoder.pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} -%kvp- %msg%n"
        patternLayoutEncoder.context = logger.loggerContext
        patternLayoutEncoder.start()

        val consoleAppender = ConsoleAppender<ILoggingEvent>()
        consoleAppender.encoder = patternLayoutEncoder
        consoleAppender.context = logger.loggerContext
        consoleAppender.name = "STDOUT"
        consoleAppender.addFilter(object : Filter<ILoggingEvent>() {
            override fun decide(event: ILoggingEvent): FilterReply {
                val levelsByLogger = mapOf(
                    "Exposed" to listOf(Level.WARN, Level.ERROR),
                    "io.netty" to listOf(Level.INFO, Level.WARN, Level.ERROR),
                    "ktor.application" to listOf(Level.WARN, Level.ERROR),
                    "org.jose4j" to listOf(Level.WARN, Level.ERROR),
                    "org.jose4j.http.Get" to listOf(Level.WARN, Level.ERROR),
                    "[Koin]" to listOf(Level.WARN, Level.ERROR),
                )
                val defaultLogLevels = when (BuildConfig.logLevel) {
                    "error" -> listOf(Level.ERROR)
                    "warn" -> listOf(Level.WARN, Level.ERROR)
                    "info" -> listOf(Level.INFO, Level.WARN, Level.ERROR)
                    else -> listOf(Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)
                }
                val levels = levelsByLogger[event.loggerName] ?: defaultLogLevels
                val isLevel = event.level in levels

                return if (isLevel) FilterReply.NEUTRAL else FilterReply.DENY
            }
        })
        if (BuildConfig.focusedLoggers.isNotBlank()) {
            val allowedLoggers = BuildConfig.focusedLoggers.split(",").map { it.trim() }
            consoleAppender.addFilter(object : Filter<ILoggingEvent>() {
                override fun decide(event: ILoggingEvent?): FilterReply {
                    return if (allowedLoggers.any { event?.loggerName?.endsWith(it) == true }) FilterReply.NEUTRAL else FilterReply.DENY
                }
            })
        }
        consoleAppender.start()

        logger.addAppender(consoleAppender)
    }
}

private fun silenceChattyThirdPartyLoggers() {
    logger.level = java.util.logging.Level.SEVERE // Turn off warnings
}
