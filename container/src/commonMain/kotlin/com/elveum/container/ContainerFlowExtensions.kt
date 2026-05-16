@file:Suppress("unused")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

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
 * Return a Container flow that switches to a new Container flow produced by transform
 * function every time the original flow emits a new container value.
 *
 * When the original flow emits a new value, the previous flow produced by transform block is
 * cancelled.
 *
 * All thrown exceptions are encapsulated into [Container.Error].
 */
public inline fun <T, R> Flow<Container<T>>.containerFlatMapLatest(
    crossinline mapper: suspend ContainerMapperScope.(T) -> Flow<Container<R>>
): Flow<Container<R>> {
    return flatMapLatest { container ->
        container.fold(
            onPending = { flowOf(pendingContainer()) },
            onError = { flowOf(errorContainer(it)) },
            onSuccess = { value ->
                try {
                    mapper(value)
                        .catch { throwable ->
                            if (throwable is Exception) {
                                emit(errorContainer(throwable))
                            } else {
                                throw throwable
                            }
                        }
                } catch (e: Exception) {
                    currentCoroutineContext().ensureActive()
                    flowOf(errorContainer(e))
                }
            }
        )
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
 * Convert the original Flow which contains a [Container] of type [T] into
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
public inline fun <T> Flow<Container<T>>.containerFilter(
    crossinline predicate: suspend ContainerMapperScope.(value: T) -> Boolean
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
    block: ContainerUpdater.() -> Unit,
): Flow<Container<T>> {
    return map { container ->
        container.update(block)
    }
}
