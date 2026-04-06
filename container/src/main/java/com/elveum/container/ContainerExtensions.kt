@file:OptIn(ExperimentalContracts::class)

package com.elveum.container

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

public typealias ListContainer<T> = Container<List<T>>

/**
 * - Returns the result of onSuccess() function if this instance is [Container.Success].
 *   If you don't provide onSuccess() implementation, the [defaultValue] is returned.
 * - Returns the result of onError() function if this instance is [Container.Error].
 *   If you don't provide onError() implementation, the [defaultValue] is returned.
 * - Returns the result of onPending() function if this instance is [Container.Pending].
 *   If you don't provide onPending() implementation, the [defaultValue] is returned.
 */
public inline fun <T, R> Container<T>.foldDefault(
    defaultValue: R,
    onPending: () -> R = { defaultValue },
    onError: ContainerMapperScope.(Exception) -> R = { defaultValue },
    onSuccess: ContainerMapperScope.(T) -> R = { defaultValue },
): R {
    return fold(
        onPending = { onPending() },
        onError = { onError(it) },
        onSuccess = { onSuccess(it) },
    )
}

/**
 * - Returns the result of onSuccess() function if this instance is [Container.Success].
 *   If you don't provide onSuccess() implementation, `null` is returned.
 * - Returns the result of onError() function if this instance is [Container.Error].
 *   If you don't provide onError() implementation, `null` is returned.
 * - Returns the result of onPending() function if this instance is [Container.Pending].
 *   If you don't provide onPending() implementation, `null` is returned.
 */
public inline fun <T, R> Container<T>.foldNullable(
    onPending: () -> R? = { null },
    onError: ContainerMapperScope.(Exception) -> R? = { null },
    onSuccess: ContainerMapperScope.(T) -> R? = { null },
): R? {
    return fold(
        onSuccess = { onSuccess(it) },
        onError = { onError(it) },
        onPending = { onPending() },
    )
}

/**
 * Get the data value encapsulated by the container if possible or return NULL.
 * If the container is [Container.Success], the wrapped value is returned.
 * Otherwise, `null` is returned.
 */
public fun <T> Container<T>.getContainerValueOrNull(): ContainerValue<T>? {
    return foldNullable {
        ContainerValue(it, metadata)
    }
}

/**
 * If the container is [Container.Error], return its encapsulated exception.
 * Otherwise, return `null`.
 */
public fun <T> Container<T>.getContainerExceptionOrNull(): ContainerValue<Exception>? {
    return foldNullable(
        onError = { ContainerValue(it, metadata) }
    )
}

/**
 * If the container is [Container.Error], return its wrapped exception.
 * Otherwise, return `null`.
 */
public fun <T> Container<T>.exceptionOrNull(): Exception? {
    return foldNullable(onError = { it })
}

/**
 * Get the value backed by the container if possible or return NULL.
 * If the container is [Container.Success], the wrapped value is returned.
 * Otherwise, `null` is returned.
 */
public fun <T> Container<T>.getOrNull(): T? {
    return foldNullable { it }
}

/**
 * Return the value encapsulated by the container if it is [Container.Success].
 * Otherwise, an exception is thrown:
 * - unwrapping a [Container.Error] throws the exception encapsulated by the container
 * - unwrapping a [Container.Pending] throws [LoadNotFinishedException]
 */
public fun <T> Container<T>.unwrap(): T {
    return unwrapContainerValue().value
}

/**
 * Return the container value encapsulated by the container if it is [Container.Success].
 * Otherwise, an exception is thrown:
 * - unwrapping a [Container.Error] throws the exception encapsulated by the container
 * - unwrapping a [Container.Pending] throws [LoadNotFinishedException]
 */
public fun <T> Container<T>.unwrapContainerValue(): ContainerValue<T> {
    return fold(
        onPending = { throw LoadNotFinishedException() },
        onError = { throw it },
        onSuccess = { ContainerValue(it, metadata) }
    )
}

/**
 * Update additional metadata in the container.
 */
public fun <T> Container<T>.update(
    block: ContainerUpdater.() -> Unit,
): Container<T> {
    return transform(
        onSuccess = { value ->
            with(applyUpdater(block)) {
                successContainer(value, metadata)
            }
        },
        onError = { exception ->
            with(applyUpdater(block)) {
                errorContainer(exception, metadata)
            }
        }
    )
}

public fun <T> Container<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is Container.Success<T>)
    }
    return this is Container.Success<T>
}

public fun <T> Container<T>.isCompleted(): Boolean {
    contract {
        returns(true) implies (this@isCompleted is Container.Completed<T>)
    }
    return this is Container.Completed<T>
}

public fun <T> Container<T>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is Container.Error)
    }
    return this is Container.Error
}

public fun <T> Container<T>.isPending(): Boolean {
    contract {
        returns(true) implies (this@isPending is Container.Pending)
    }
    return this is Container.Pending
}

public fun <T> Container<T>.isDataLoading(): Boolean = isPending() || backgroundLoadState == BackgroundLoadState.Loading

private fun ContainerMapperScope.applyUpdater(block: ContainerUpdater.() -> Unit): ContainerUpdater {
    val updater = ContainerUpdaterImpl(this)
    updater.apply(block)
    return updater
}
