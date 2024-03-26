package dev.nohus.rift

import dev.nohus.rift.crash.handleFatalException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

open class ViewModel {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> handleFatalException(throwable) }
    protected val viewModelScope = CoroutineScope(Job() + exceptionHandler)
}
