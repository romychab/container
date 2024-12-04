package com.elveum.container

public class LoadInProgressException : IllegalStateException("Container is Pending and can't be unwrapped")

public typealias ListContainer<T> = Container<List<T>>

/**
 * If the container is [Container.Error], return its wrapped exception.
 * Otherwise, return NULL.
 */
public fun <T> Container<T>.exceptionOrNull(): Throwable? {
    return if (this is Container.Error) {
        exception
    } else {
        null
    }
}

/**
 * Get the value backed by the container if possible or throw exception.
 * If the container is [Container.Success], the wrapped value is returned.
 * If the container is [Container.Pending], [IllegalStateException] is thrown.
 * If the container is [Container.Error], the wrapped exception is thrown.
 */
public fun <T> Container<T>.unwrap(): T {
    return unwrapData().value
}

/**
 * Get the value backed by the container if possible or throw exception.
 * - If the container is [Container.Success], the wrapped value along
 *   ith its source indicator is returned.
 * - If the container is [Container.Pending], [IllegalStateException] is thrown.
 * - If the container is [Container.Error], the wrapped exception is thrown.
 */
public fun <T> Container<T>.unwrapData(): SourceValue<T> {
    return when (this) {
        is Container.Pending -> throw LoadInProgressException()
        is Container.Error -> throw exception
        is Container.Success -> SourceValue(value, source)
    }
}

/**
 * Try to get the value backed by the container.
 * - If the container is [Container.Success], the wrapped value is returned.
 * - If the container is [Container.Pending], NULL is returned.
 * - If the container is [Container.Error], the wrapped exception is thrown.
 */
public fun <T> Container<T>.unwrapOrNull(): T? {
    return unwrapDataOrNull()?.value
}

/**
 * Try to get the value backed by the container.
 * - If the container is [Container.Success], the wrapped value along with
 *   its source indicator is returned.
 * - If the container is [Container.Pending], NULL is returned.
 * - If the container is [Container.Error], the wrapped exception is thrown.
 */
public fun <T> Container<T>.unwrapDataOrNull(): SourceValue<T>? {
    return when (this) {
        is Container.Pending -> null
        is Container.Error -> throw exception
        is Container.Success -> SourceValue(value, source)
    }
}

/**
 * Get the value backed by the container if possible or return NULL.
 * If the container is [Container.Success], the wrapped value is returned.
 * Otherwise, NULL is returned.
 */
public fun <T> Container<T>.getOrNull(): T? {
    return getDataOrNull()?.value
}

/**
 * Get the data value backed by the container if possible or return NULL.
 * If the container is [Container.Success], the wrapped value along with its
 * source indicator is returned.
 * Otherwise, NULL is returned.
 */
public fun <T> Container<T>.getDataOrNull(): SourceValue<T>? {
    return when (this) {
        is Container.Success -> SourceValue(value, source)
        else -> null
    }
}
