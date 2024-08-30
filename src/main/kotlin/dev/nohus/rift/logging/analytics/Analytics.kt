package dev.nohus.rift.logging.analytics

open class Analytics {
    open suspend fun start() {}
    open fun alertTriggered() {}
}
