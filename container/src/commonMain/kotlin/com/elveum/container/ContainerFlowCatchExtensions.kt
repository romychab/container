@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlin.reflect.KClass


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


/**
 * Recover encapsulated [Container.Error] instances with [clazz] exception emitted by
 * the flow, and transform them into success containers using the [mapper] function.
 *
 * @see Container.catch
 */
public inline fun <T, E : Exception> Flow<Container<T>>.containerRecover(
    clazz: KClass<E>,
    crossinline mapper: suspend (E) -> T,
): Flow<Container<T>> {
    return mapLatest { container ->
        container.recover(clazz) { mapper(it) }
    }
}
