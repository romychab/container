package com.elveum.container.factory

import com.elveum.container.cache.CacheValueLoader
import com.elveum.container.cache.LazyCache
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation

public open class DefaultContainerFactory(
    private val cacheTimeoutMillis: Long = DefaultCacheTimeoutMillis,
    private val coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
    private val transformationFactory: TransformationFactory = TransformationFactory,
) : ContainerFactory {

    override fun <T> createSubject(
        cacheTimeoutMillis: Long?,
        coroutineScopeFactory: CoroutineScopeFactory?,
        transformation: ContainerTransformation<T>?,
        valueLoader: ValueLoader<T>
    ): LazyFlowSubject<T> {
        return LazyFlowSubject.create(
            valueLoader = valueLoader,
            cacheTimeoutMillis = cacheTimeoutMillis ?: this.cacheTimeoutMillis,
            coroutineScopeFactory = coroutineScopeFactory ?: this.coroutineScopeFactory,
            transformation = transformation ?: transformationFactory.create(),
        )
    }

    override fun <Arg, T> createCache(
        cacheTimeoutMillis: Long?,
        coroutineScopeFactory: CoroutineScopeFactory?,
        transformation: ContainerTransformation<T>?,
        valueLoader: CacheValueLoader<Arg, T>
    ): LazyCache<Arg, T> {
        return LazyCache.create(
            valueLoader = valueLoader,
            cacheTimeoutMillis = cacheTimeoutMillis ?: this.cacheTimeoutMillis,
            coroutineScopeFactory = coroutineScopeFactory ?: this.coroutineScopeFactory,
            transformation = transformation ?: transformationFactory.create(),
        )
    }

}
