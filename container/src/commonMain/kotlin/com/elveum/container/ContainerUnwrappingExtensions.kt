package com.elveum.container

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
