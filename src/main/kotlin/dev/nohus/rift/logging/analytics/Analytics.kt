package dev.nohus.rift.logging.analytics

open class Analytics {
    open suspend fun start() {}
    open fun windowOpened(name: String) {}
    open fun windowClosed(name: String) {}
    open fun alertTriggered() {}
}
