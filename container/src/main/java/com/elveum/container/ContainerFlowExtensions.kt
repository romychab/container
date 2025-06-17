@file:Suppress("unused")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlin.reflect.KClass

public typealias ContainerFlow<T> = Flow<Container<T>>
public typealias ListContainerFlow<T> = Flow<ListContainer<T>>

/**
 * Convert the original Flow which contains a [Container] of type [T] into
 * a Flow which also contains a [Container] but of type [R].
 */
public inline fun <T, R> Flow<Container<T>>.containerMap(
    crossinline mapper: suspend ContainerMapperScope.(T) -> R,
): Flow<Container<R>> {
    return map { container ->
        container.map { mapper(it) }
    }
}

/**
 * Convert the original StateFlow which contains a [Container] of type [T] into
 * a StateFlow which also contains a [Container] but of type [R].
 */
public inline fun <T, R> StateFlow<Container<T>>.containerStateMap(
    crossinline mapper: ContainerMapperScope.(T) -> R
): StateFlow<Container<R>> {
    return stateMap { container -> container.map(mapper = mapper) }
}

/**
 * Convert the original Flow which contains a [Container] of type [T] info
 * a Flow which also contains a [Container] but of type [R].
 *
 * When the original flow emits a new value, computation for the previous
 * value is cancelled.
 */
public inline fun <T, R> Flow<Container<T>>.containerMapLatest(
    crossinline mapper: suspend ContainerMapperScope.(T) -> R,
): Flow<Container<R>> {
    return mapLatest { container ->
        container.map { mapper(it) }
    }
}

/**
 * Returns a flow containing the filtered values of the original flow:
 * - if the input value is [Container.Pending] then it is sent to a new flow
 * - if the input value is [Container.Error] then it is sent to a new flow too
 * - if the input value is [Container.Success] then its wrapped value is
 *   filtered by the [predicate]. If the [predicate] returns TRUE for the
 *   wrapped value then it is sent to a new flow too.
 */
public fun <T> Flow<Container<T>>.containerFilter(
    predicate: suspend ContainerMapperScope.(value: T) -> Boolean
): Flow<Container<T>> {
    return filter { container ->
        container.foldDefault(
            defaultValue = true,
            onSuccess = { predicate(this, it) }
        )
    }
}

/**
 * Returns a flow containing the filtered values of the original flow:
 * - if the input value is [Container.Pending] then it is sent to a new flow
 * - if the input value is [Container.Error] then it is sent to a new flow too
 * - if the input value is [Container.Success] then its wrapped value is
 *   filtered by the [predicate]. If the [predicate] returns FALSE for the
 *   wrapped value then it is sent to a new flow too.
 */
public inline fun <T> Flow<Container<T>>.containerFilterNot(
    crossinline predicate: suspend ContainerMapperScope.(value: T) -> Boolean
): Flow<Container<T>> {
    return containerFilter { predicate(it).not() }
}

/**
 * Update additional data in all containers emitted by the flow.
 */
public fun <T> Flow<Container<T>>.containerUpdate(
    source: SourceType? = null,
    reloadFunction: ReloadFunction? = null,
    isLoadingInBackground: Boolean? = null,
): Flow<Container<T>> {
    return map { container ->
        container.update(source, reloadFunction, isLoadingInBackground)
    }
}

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

/**
 * Catch encapsulated error containers within the flow and transform them into other
 * containers using the [mapper] function.
 *
 * @see Container.catchAll
 */
public inline fun <T> Flow<Container<T>>.containerCatchAll(
    crossinline mapper: suspend ContainerMapperScope.(Exception) -> Container<T>,
): Flow<Container<T>> {
    return mapLatest { container ->
        container.catchAll { mapper(it) }
    }
}

/**
 * Catch encapsulated [Container.Error] instances with [clazz] exception emitted by
 * the flow, and transform them into other container using the [mapper] function.
 *
 * @see Container.catch
 */
public inline fun <T, E : Exception> Flow<Container<T>>.containerCatch(
    clazz: KClass<E>,
    crossinline mapper: suspend ContainerMapperScope.(E) -> Container<T>,
): Flow<Container<T>> {
    return mapLatest { container ->
        container.catch(clazz) { mapper(it) }
    }
}

/**
 * Map all encapsulated exceptions of type [E] emitted by the flow to other exception
 * types using the [mapper] function.
 *
 * @see Container.mapException
 */
public inline fun <T, E : Exception> Flow<Container<T>>.containerMapException(
    clazz: KClass<E>,
    crossinline mapper: suspend (E) -> Exception,
): Flow<Container<T>> {
    return mapLatest { container ->
        container.mapException(clazz) { mapper(it) }
    }
}
