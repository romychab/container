@file:Suppress("unused")

package com.elveum.container

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout

public typealias ContainerFlow<T> = Flow<Container<T>>
public typealias ListContainerFlow<T> = Flow<ListContainer<T>>

/**
 * Wait for the first non-pending container and return its result.
 * - If that container is [Container.Success] then its wrapped value is returned.
 * - If that container is [Container.Error] then its wrapped exception is thrown.
 *
 * If [timeoutMillis] is specified and the flow hasn't emit either [Container.Success]
 * or [Container.Error] instances then [TimeoutCancellationException] is thrown.
 *
 * If the origin flow completes without emitting [Container.Error] or [Container.Success]
 * values then [NoSuchElementException] is thrown.
 */
public suspend fun <T> Flow<Container<T>>.unwrapFirst(timeoutMillis: Long? = null): T {
    val flow = this
    val block: suspend () -> T = {
        val container = flow
            .filterNot { it is Container.Pending }
            .first()
        container.unwrap()
    }
    return if (timeoutMillis == null) {
        block()
    } else {
        withTimeout(timeoutMillis) { block() }
    }
}

/**
 * Wait for the first non-pending container and return its result.
 * - If that container is [Container.Success] then its wrapped value is returned.
 * - If that container is [Container.Error] OR if the flow completes without
 *   emitting either [Container.Success] or [Container.Error] then [orElse] block
 *   is called and its result is returned.
 *
 * If [timeoutMillis] is specified and the flow hasn't emit either [Container.Success]
 * or [Container.Error] instances then [TimeoutCancellationException] is thrown.
 */
public suspend fun <T> Flow<Container<T>>.unwrapFirstOrElse(
    timeoutMillis: Long? = null,
    orElse: () -> T
): T {
    return try {
        unwrapFirst(timeoutMillis)
    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
            return orElse()
        } else {
            throw e
        }
    } catch (e: Throwable) {
        orElse()
    }
}

/**
 * Wait for the first non-pending container and return its result.
 * - If that container is [Container.Success] then its wrapped value is returned.
 * - If that container is [Container.Error] OR if the flow completes without
 *   emitting either [Container.Success] or [Container.Error]
 *   then [defaultValue] is returned.
 *
 * If [timeoutMillis] is specified and the flow hasn't emit either [Container.Success]
 * or [Container.Error] instances then [TimeoutCancellationException] is thrown.
 */
public suspend fun <T> Flow<Container<T>>.unwrapFirstOrDefault(
    timeoutMillis: Long? = null,
    defaultValue: T
): T {
    return unwrapFirstOrElse(timeoutMillis) { defaultValue }
}

/**
 * Convert the original Flow which contains a [Container] of type [T] into
 * a Flow which also contains a [Container] but of type [R].
 */
public inline fun <T, R> Flow<Container<T>>.containerMap(
    crossinline mapper: ContainerMapper<T, R>
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
    crossinline mapper: ContainerMapper<T, R>
): StateFlow<Container<R>> {
    return stateMap { container -> container.map(mapper) }
}

/**
 * Convert the original Flow which contains a [Container] of type [T] info
 * a Flow which also contains a [Container] but of type [R].
 *
 * When the original flow emits a new value, computation for the previous
 * value is cancelled.
 */
@Suppress("OPT_IN_USAGE")
public inline fun <T, R> Flow<Container<T>>.containerMapLatest(
    crossinline mapper: ContainerMapper<T, R>
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
        when (container) {
            is Container.Pending -> true
            is Container.Error -> true
            is Container.Success -> predicate(container, container.value)
        }
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
