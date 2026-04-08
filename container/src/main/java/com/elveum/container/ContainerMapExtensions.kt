package com.elveum.container

import kotlin.coroutines.cancellation.CancellationException

/**
 * Transform the container into another container.
 *
 * - [Container.Pending] is mapped to itself
 * - [Container.Error] is mapped to container produced by [onError] function.
 * - [Container.Success] is mapped to container produced by [onSuccess] function.
 *
 * In case of mapping failure, the container is mapped to [Container.Error].
 *
 * All additional container data (reload function, source type, loading status)
 * is retained automatically.
 */
public inline fun <T, R> Container<T>.transform(
    onError: ContainerMapperScope.(Exception) -> Container<R> = { errorContainer(it) },
    onSuccess: ContainerMapperScope.(T) -> Container<R>,
): Container<R> {
    return fold(
        onSuccess = {
            try {
                onSuccess(it)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorContainer(e)
            }
        },
        onError = {
            try {
                onError(it)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorContainer(e)
            }
        },
        onPending = { Container.Pending }
    )
}

/**
 * Transform the completed container into another completed container.
 *
 * - [Container.Error] is mapped to container produced by [onError] function.
 * - [Container.Success] is mapped to container produced by [onSuccess] function.
 *
 * In case of mapping failure, the container is mapped to [Container.Error].
 *
 * All additional container data (reload function, source type, loading status)
 * is retained automatically.
 */
public inline fun <T, R> Container.Completed<T>.transform(
    onError: ContainerMapperScope.(Exception) -> Container.Completed<R> = { errorContainer(it) },
    onSuccess: ContainerMapperScope.(T) -> Container.Completed<R>,
): Container.Completed<R> {
    return fold(
        onSuccess = {
            try {
                onSuccess(it)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorContainer(e)
            }
        },
        onError = {
            try {
                onError(it)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorContainer(e)
            }
        },
    )
}

/**
 * Convert the container type to another type by using a lambda [mapper].
 */
public inline fun <T, R> Container<T>.map(
    mapper: ContainerMapperScope.(T) -> R,
): Container<R> {
    return transform {
        successContainer(mapper(it))
    }
}

/**
 * Convert the completed container type to another type by using a lambda [mapper].
 */
public inline fun <T, R> Container.Completed<T>.map(
    mapper: ContainerMapperScope.(T) -> R,
): Container.Completed<R> {
    return transform {
        successContainer(mapper(it))
    }
}
