package com.elveum.container

import kotlin.reflect.KClass


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

/**
 * Recover all encapsulated exceptions of type [E] by using [mapper] function.
 */
public inline fun <T, E : Exception> Container<T>.recover(
    clazz: KClass<E>,
    mapper: (E) -> T,
): Container<T> {
    return catch(clazz) { exception ->
        successContainer(mapper(exception))
    }
}

/**
 * Recover all encapsulated exceptions of type [E] by using [mapper] function.
 */
public inline fun <T, E : Exception> Container.Completed<T>.recover(
    clazz: KClass<E>,
    mapper: (E) -> T,
): Container.Completed<T> {
    return catch(clazz) { exception ->
        successContainer(mapper(exception))
    }
}
