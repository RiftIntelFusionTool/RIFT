package dev.nohus.rift.network

sealed class Result<out T : Any?> {
    data class Success<out T : Any?>(val data: T) : Result<T>()
    data class Failure(val cause: Exception? = null) : Result<Nothing>()

    val success get() = (this as? Success<T>)?.data
    val failure get() = (this as? Failure)?.cause
    val isSuccess get() = this is Success
    val isFailure get() = this is Failure
    val successOrThrow get() = when (this) {
        is Success -> this.data
        is Failure -> throw this.cause ?: Exception("Unknown failure")
    }

    inline fun <R : Any?> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Failure -> Failure(cause)
        }
    }

    inline fun mapFailure(transform: (Exception?) -> Exception?): Result<T> {
        return when (this) {
            is Success -> this
            is Failure -> Failure(transform(this.cause))
        }
    }

    inline fun <R : Any?> mapResult(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(data)
            is Failure -> Failure(cause)
        }
    }
}
