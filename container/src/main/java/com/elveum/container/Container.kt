package com.elveum.container

/**
 * Represents the current status of async fetch/operation.
 * @see Container.Pending
 * @see Container.Error
 * @see Container.Success
 */
public sealed class Container<out T> {

    /**
     * - Returns the result of onSuccess() function if this instance is [Container.Success].
     * - Returns the result of onError() function if this instance is [Container.Error].
     * - Returns the result of onPending() function if this instance is [Container.Pending].
     */
    public inline fun <R> fold(
        onPending: () -> R,
        onError: ContainerMapperScope.(Exception) -> R,
        onSuccess: ContainerMapperScope.(T) -> R,
    ): R {
        return when (this) {
            is Success<T> -> onSuccess(this, value)
            Pending -> onPending()
            is Error -> onError(this, exception)
        }
    }

    public sealed class Completed<out T> : Container<T>(), ContainerMapperScope {

        /**
         * The original source where the data is loading from.
         */
        public abstract override val source: SourceType

        /**
         * Function for reloading data.
         */
        public abstract override val reloadFunction: ReloadFunction

        /**
         * Whether there is another value load in progress.
         * For example, it may be a value being loaded from the remote source, whereas
         * this container represents a local source.
         */
        public abstract override val isLoadingInBackground: Boolean

        /**
         * - Returns the result of onSuccess() function if this instance is [Container.Success].
         * - Returns the result of onError() function if this instance is [Container.Error].
         */
        public inline fun <R> fold(
            onError: ContainerMapperScope.(Exception) -> R,
            onSuccess: ContainerMapperScope.(T) -> R,
        ): R {
            return when (this) {
                is Success<T> -> onSuccess(this, value)
                is Error -> onError(this, exception)
            }
        }
    }

    /**
     * The operation is still in progress.
     */
    public data object Pending : Container<Nothing>()

    /**
     * The operation has been finished with success.
     */
    public data class Success<T>
    @Deprecated("Use successContainer() function instead of direct call of constructor.")
    constructor(
        val value: T,
        override val source: SourceType = UnknownSourceType,
        override val isLoadingInBackground: Boolean = false,
        override val reloadFunction: ReloadFunction = EmptyReloadFunction,
    ) : Completed<T>() {
        override fun toString(): String {
            return "Success($value)"
        }
    }

    /**
     * The operation has been failed.
     */
    public data class Error
    @Deprecated("Use errorContainer() function instead of direct call of constructor.")
    constructor(
        val exception: Exception,
        override val source: SourceType = UnknownSourceType,
        override val isLoadingInBackground: Boolean = false,
        override val reloadFunction: ReloadFunction = EmptyReloadFunction,
    ) : Completed<Nothing>() {
        override fun toString(): String {
            return "Error($exception)"
        }
    }

}

/**
 * Create a pending container.
 */
public fun pendingContainer(): Container.Pending {
    return Container.Pending
}

/**
 * Create a success container.
 */
public fun <T> successContainer(
    value: T,
    source: SourceType = UnknownSourceType,
    isLoadingInBackground: Boolean = false,
    reloadFunction: ReloadFunction = EmptyReloadFunction,
): Container.Success<T> {
    @Suppress("DEPRECATION")
    return Container.Success(value, source, isLoadingInBackground, reloadFunction)
}

/**
 * Create an error container.
 */
public fun errorContainer(
    exception: Exception,
    source: SourceType = UnknownSourceType,
    isLoadingInBackground: Boolean = false,
    reloadFunction: ReloadFunction = EmptyReloadFunction,
): Container.Error {
    @Suppress("DEPRECATION")
    return Container.Error(exception, source, isLoadingInBackground, reloadFunction)
}
