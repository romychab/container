package com.elveum.container.factory

import com.elveum.container.Container
import com.elveum.container.SourceType
import com.elveum.container.UnknownSourceType
import com.elveum.container.cache.LazyCache
import com.elveum.container.cache.SimpleCacheValueLoader
import com.elveum.container.subject.ContainerConfiguration
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.SimpleValueLoader
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation
import kotlinx.coroutines.flow.Flow

/**
 * Execute and cache [valueLoader] lazily when at least 1 subscriber
 * starts collecting a flow.
 */
public fun <T> ContainerFactory.createFlow(
    configuration: ContainerConfiguration = ContainerConfiguration(),
    cacheTimeoutMillis: Long? = null,
    coroutineScopeFactory: CoroutineScopeFactory? = null,
    transformation: ContainerTransformation<T>? = null,
    valueLoader: ValueLoader<T>
): Flow<Container<T>> {
    return createSubject(
        cacheTimeoutMillis,
        coroutineScopeFactory,
        transformation,
        valueLoader
    ).listen(configuration)
}

/**
 * Execute and cache [valueLoader] lazily when at least 1 subscriber
 * starts collecting a flow.
 */
public fun <T> ContainerFactory.createSimpleSubject(
    sourceType: SourceType = UnknownSourceType,
    valueLoader: SimpleValueLoader<T>,
): LazyFlowSubject<T> {
    return createSubject {
        emit(valueLoader(), sourceType, isLastValue = true)
    }
}

/**
 * Execute and cache [valueLoader] lazily when at least 1 subscriber
 * starts collecting a flow.
 */
public fun <T> ContainerFactory.createSimpleFlow(
    sourceType: SourceType = UnknownSourceType,
    valueLoader: SimpleValueLoader<T>
): Flow<Container<T>> {
    return createFlow {
        emit(valueLoader(), sourceType, isLastValue = true)
    }
}

/**
 * Execute and cache [valueLoader] lazily when at least 1 subscriber
 * starts collecting a flow.
 */
public fun <Arg, T> ContainerFactory.createSimpleCache(
    sourceType: SourceType = UnknownSourceType,
    valueLoader: SimpleCacheValueLoader<Arg, T>,
): LazyCache<Arg, T> {
    return createCache { arg ->
        emit(valueLoader(arg), sourceType, isLastValue = true)
    }
}
