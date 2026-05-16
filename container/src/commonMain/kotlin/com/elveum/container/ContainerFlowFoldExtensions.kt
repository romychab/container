@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest


/**
 * Fold containers emitted by the flow.
 *
 * @see Container.fold
 */
public inline fun <T, R> Flow<Container<T>>.containerFold(
    crossinline onSuccess: suspend ContainerMapperScope.(T) -> R,
    crossinline onError: suspend ContainerMapperScope.(Exception) -> R,
    crossinline onPending: suspend () -> R,
): Flow<R> {
    return mapLatest { container ->
        container.fold(
            onSuccess = { onSuccess(it) },
            onError = { onError(it) },
            onPending = { onPending() }
        )
    }
}

/**
 * Fold all containers emitted by the flow. If the corresponding callback
 * is not specified, the [defaultValue] is emitted.
 *
 * @see Container.foldDefault
 */
public inline fun <T, R> Flow<Container<T>>.containerFoldDefault(
    defaultValue: R,
    crossinline onSuccess: suspend ContainerMapperScope.(T) -> R = { defaultValue },
    crossinline onError: suspend ContainerMapperScope.(Exception) -> R = { defaultValue },
    crossinline onPending: suspend () -> R = { defaultValue },
): Flow<R> {
    return containerFold(onSuccess, onError, onPending)
}

/**
 * Fold all containers emitted by the flow. If the corresponding callback
 * is not specified, `null` is emitted.
 *
 * @see Container.foldNullable
 */
public inline fun <T, R> Flow<Container<T>>.containerFoldNullable(
    crossinline onSuccess: suspend ContainerMapperScope.(T) -> R? = { null },
    crossinline onError: suspend ContainerMapperScope.(Exception) -> R? = { null },
    crossinline onPending: suspend () -> R? = { null },
): Flow<R?> {
    return containerFold(onSuccess, onError, onPending)
}


/**
 * Transform all encapsulated containers by using [onSuccess] and
 * [onError] functions.
 *
 * @see Container.transform
 */
public inline fun <T, R> Flow<Container<T>>.containerTransform(
    crossinline onSuccess: suspend ContainerMapperScope.(T) -> Container<R>,
    crossinline onError: suspend ContainerMapperScope.(Exception) -> Container<R>,
): Flow<Container<R>> {
    return mapLatest { container ->
        container.transform(
            onSuccess = { onSuccess(it) },
            onError = { onError(it) },
        )
    }
}
