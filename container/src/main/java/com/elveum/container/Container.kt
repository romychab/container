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
     * Append the specified [metadata] to the container.
     */
    public abstract operator fun plus(metadata: ContainerMetadata): Container<T>

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
        public abstract override operator fun plus(metadata: ContainerMetadata): Completed<T>

    }

    /**
     * The operation is still in progress.
     */
    public data object Pending : Container<Nothing>() {
        override val metadata: ContainerMetadata = EmptyMetadata
        override fun filterMetadata(predicate: (ContainerMetadata) -> Boolean): Pending = Pending
        override fun raw(): Pending = Pending
        override fun plus(metadata: ContainerMetadata): Pending = Pending
    }

    /**
     * The operation has been finished with success.
     */
    @ExposedCopyVisibility
    public data class Success<T> internal constructor(
        val value: T,
        override val metadata: ContainerMetadata,
    ) : Completed<T>() {

        @Deprecated("Use successContainer() function instead of direct call of constructor.")
        public constructor(
            value: T,
            source: SourceType? = null,
            isLoadingInBackground: Boolean? = null,
            reloadFunction: ReloadFunction? = null,
        ) : this(
            value = value,
            metadata = defaultMetadata(source, isLoadingInBackground, reloadFunction)
        )

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

        @Deprecated("Use errorContainer() function instead of direct call of constructor.")
        public constructor(
            exception: Exception,
            source: SourceType? = null,
            isLoadingInBackground: Boolean? = null,
            reloadFunction: ReloadFunction? = null,
        ) : this(
            exception = exception,
            metadata = defaultMetadata(source, isLoadingInBackground, reloadFunction)
        )

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
@Deprecated("Use successContainer() with metadata argument.")
public fun <T> successContainer(
    value: T,
    source: SourceType,
    isLoadingInBackground: Boolean? = null,
    reloadFunction: ReloadFunction? = null,
): Container.Success<T> {
    return Container.Success(
        value = value,
        metadata = defaultMetadata(source, isLoadingInBackground, reloadFunction),
    )
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
 * Create a success container.
 */
@Deprecated("Use successContainer() with metadata argument.")
public fun <T> successContainer(
    value: T,
    isLoadingInBackground: Boolean,
): Container.Success<T> {
    return Container.Success(
        value = value,
        metadata = defaultMetadata(isLoadingInBackground = isLoadingInBackground),
    )
}

/**
 * Create a success container.
 */
@Deprecated("Use successContainer() with metadata argument.")
public fun <T> successContainer(
    value: T,
    reloadFunction: ReloadFunction,
): Container.Success<T> {
    return Container.Success(
        value = value,
        metadata = defaultMetadata(reloadFunction = reloadFunction),
    )
}

/**
 * Create an error container.
 */
@Deprecated("Use errorContainer() with metadata argument.")
public fun errorContainer(
    exception: Exception,
    source: SourceType,
    isLoadingInBackground: Boolean? = null,
    reloadFunction: ReloadFunction? = null,
): Container.Error {
    return Container.Error(
        exception = exception,
        metadata = defaultMetadata(source, isLoadingInBackground, reloadFunction),
    )
}

/**
 * Create an error container.
 */
@Deprecated("Use errorContainer() with metadata argument.")
public fun errorContainer(
    exception: Exception,
    isLoadingInBackground: Boolean,
): Container.Error {
    return Container.Error(
        exception = exception,
        metadata = defaultMetadata(isLoadingInBackground = isLoadingInBackground),
    )
}


/**
 * Create an error container.
 */
@Deprecated("Use errorContainer() with metadata argument.")
public fun errorContainer(
    exception: Exception,
    reloadFunction: ReloadFunction,
): Container.Error {
    return Container.Error(
        exception = exception,
        metadata = defaultMetadata(reloadFunction = reloadFunction),
    )
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
