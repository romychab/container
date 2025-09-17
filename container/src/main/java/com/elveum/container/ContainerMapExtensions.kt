package com.elveum.container

import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

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
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                errorContainer(e)
            }
        },
        onError = {
            try {
                onError(it)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                errorContainer(e)
            }
        },
        onError = {
            try {
                onError(it)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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

/**
 * Catch encapsulated [Container.Error] and transform it into other container
 * using the [mapper] function. Other container types are not affected.
 */
public inline fun <T> Container<T>.catchAll(
    mapper: ContainerMapperScope.(Exception) -> Container<T>,
): Container<T> {
    return transform(
        onSuccess = { successContainer(it) },
        onError = { exception -> mapper(exception) },
    )
}

/**
 * Catch encapsulated [Container.Error] and transform it into other container
 * using the [mapper] function. Other container types are not affected.
 */
public inline fun <T> Container.Completed<T>.catchAll(
    mapper: ContainerMapperScope.(Exception) -> Container.Completed<T>,
): Container.Completed<T> {
    return transform(
        onSuccess = { successContainer(it) },
        onError = { exception -> mapper(exception) },
    )
}

/**
 * Catch encapsulated [Container.Error] (if it contains [clazz] exception)
 * and transform it into other container using the [mapper] function.
 * Other container types and error containers that have another exception type are not affected.
 */
public inline fun <T, E : Exception> Container<T>.catch(
    clazz: KClass<E>,
    mapper: ContainerMapperScope.(E) -> Container<T>,
): Container<T> {
    return catchAll { exception ->
        if (clazz.isInstance(exception)) {
            @Suppress("UNCHECKED_CAST")
            mapper(exception as E)
        } else {
            errorContainer(exception)
        }
    }
}


/**
 * Catch encapsulated [Container.Error] (if it contains [clazz] exception)
 * and transform it into other container using the [mapper] function.
 * Other container types and error containers that have another exception type are not affected.
 */
public inline fun <T, E : Exception> Container.Completed<T>.catch(
    clazz: KClass<E>,
    mapper: ContainerMapperScope.(E) -> Container.Completed<T>,
): Container.Completed<T> {
    return catchAll { exception ->
        if (clazz.isInstance(exception)) {
            @Suppress("UNCHECKED_CAST")
            mapper(exception as E)
        } else {
            errorContainer(exception)
        }
    }
}

/**
 * Map all encapsulated exceptions of type [E] to other exception types
 * by using the [mapper] function.
 */
public inline fun <T, E : Exception> Container<T>.mapException(
    clazz: KClass<E>,
    mapper: (E) -> Exception,
): Container<T> {
    return catch(clazz) { exception ->
        errorContainer(mapper(exception))
    }
}

/**
 * Map all encapsulated exceptions of type [E] to other exception types
 * by using the [mapper] function.
 */
public inline fun <T, E : Exception> Container.Completed<T>.mapException(
    clazz: KClass<E>,
    mapper: (E) -> Exception,
): Container.Completed<T> {
    return catch(clazz) { exception ->
        errorContainer(mapper(exception))
    }
}
