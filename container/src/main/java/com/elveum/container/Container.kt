package com.elveum.container

/**
 * Represents the current status of async fetch/operation.
 * @see Container.Pending
 * @see Container.Error
 * @see Container.Success
 */
public sealed class Container<out T> {

    public abstract val metadata: ContainerMetadata

    /**
     * Whether there is another value load in progress.
     * For example, it may be a value being loaded from the remote source, whereas
     * this container represents a local source.
     */
    public abstract val backgroundLoadState: BackgroundLoadState

    /**
     * Reload data encapsulated by container.
     *
     * @param config defines how the loading state will be propagated to subsequent containers.
     */
    public abstract fun reload(config: LoadConfig)

    /**
     * Reload data encapsulated by container.
     */
    public abstract fun reload()

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

    /**
     * Get a container instance with filtered metadata.
     */
    public abstract fun filterMetadata(
        predicate: (ContainerMetadata) -> Boolean,
    ): Container<T>

    /**
     * Get a modified container without metadata.
     */
    public abstract fun raw(): Container<T>

    /**
     * A container representing already completed operation (either success, or error).
     */
    public sealed class Completed<out T> : Container<T>(), ContainerMapperScope {

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

        /**
         * Get a container instance with filtered metadata.
         */
        abstract override fun filterMetadata(predicate: (ContainerMetadata) -> Boolean): Completed<T>

        /**
         * Get the container without internal metadata.
         */
        abstract override fun raw(): Completed<T>

        /**
         * Append the specified [metadata] to the container.
         */
        public abstract operator fun plus(metadata: ContainerMetadata): Completed<T>

    }

    /**
     * The operation is still in progress.
     */
    public data object Pending : Container<Nothing>() {
        override val metadata: ContainerMetadata = EmptyMetadata
        override val backgroundLoadState: BackgroundLoadState = BackgroundLoadState.Idle
        override fun filterMetadata(predicate: (ContainerMetadata) -> Boolean): Pending = Pending
        override fun raw(): Pending = Pending
        override fun reload(config: LoadConfig): Unit = Unit
        override fun reload(): Unit = Unit
    }

    /**
     * The operation has been finished with success.
     */
    @ExposedCopyVisibility
    public data class Success<T> internal constructor(
        val value: T,
        override val metadata: ContainerMetadata,
    ) : Completed<T>() {

        override fun filterMetadata(predicate: (ContainerMetadata) -> Boolean): Completed<T> {
            return copy(value = value, metadata = metadata.filter(predicate))
        }

        override fun raw(): Completed<T> {
            return filterMetadata { false }
        }

        override fun plus(metadata: ContainerMetadata): Success<T> {
            return copy(metadata = this.metadata + metadata)
        }

        override fun toString(): String {
            return "Success($value, metadata=$metadata)"
        }
    }

    /**
     * The operation has been failed.
     */
    @ExposedCopyVisibility
    public data class Error internal constructor(
        val exception: Exception,
        override val metadata: ContainerMetadata,
    ) : Completed<Nothing>() {

        override fun filterMetadata(predicate: (ContainerMetadata) -> Boolean): Error {
            return copy(exception = exception, metadata = metadata.filter(predicate))
        }

        override fun raw(): Error {
            return filterMetadata { false }
        }

        override fun plus(metadata: ContainerMetadata): Error {
            return copy(metadata = this.metadata + metadata)
        }

        override fun toString(): String {
            return "Error($exception, metadata=$metadata)"
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
    metadata: ContainerMetadata = defaultMetadata(),
): Container.Success<T> {
    return Container.Success(value, metadata)
}

/**
 * Create an error container.
 */
public fun errorContainer(
    exception: Exception,
    metadata: ContainerMetadata = defaultMetadata(),
): Container.Error {
    return Container.Error(exception, metadata)
}
