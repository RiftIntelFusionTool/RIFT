package dev.nohus.rift.network

sealed class AsyncResource<out T> {
    class Ready<T>(val value: T) : AsyncResource<T>()
    class Error(val exception: Throwable?) : AsyncResource<Nothing>()
    data object Loading : AsyncResource<Nothing>()

    val success get() = (this as? Ready<T>)?.value

    fun <R> map(transform: (T) -> R): AsyncResource<R> {
        return when (this) {
            is Ready -> {
                try {
                    Ready(transform(value))
                } catch (e: Exception) {
                    Error(e)
                }
            }
            is Error -> this
            Loading -> Loading
        }
    }
}

fun <T> Result<T>.toResource(): AsyncResource<T> {
    return when (this) {
        is Result.Success -> AsyncResource.Ready(data)
        is Result.Failure -> AsyncResource.Error(cause)
    }
}
