package com.elveum.container.factory

import com.elveum.container.cache.CacheValueLoader
import com.elveum.container.cache.LazyCache
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation

public const val DefaultCacheTimeoutMillis: Long = 1000L

/**
 * Factory that can create [LazyFlowSubject] and [LazyCache] instances.
 */
public interface ContainerFactory {

    /**
     * Create a new [LazyFlowSubject] instance which loads
     * data by using the specified [valueLoader].
     */
    public fun <T> createSubject(
        cacheTimeoutMillis: Long? = null,
        coroutineScopeFactory: CoroutineScopeFactory? = null,
        transformation: ContainerTransformation<T>? = null,
        valueLoader: ValueLoader<T>
    ): LazyFlowSubject<T>

    /**
     * Create a new [LazyCache] instance which loads
     * data by using the specified [valueLoader].
     */
    public fun <Arg, T> createCache(
        cacheTimeoutMillis: Long? = null,
        coroutineScopeFactory: CoroutineScopeFactory? = null,
        transformation: ContainerTransformation<T>? = null,
        valueLoader: CacheValueLoader<Arg, T>,
    ): LazyCache<Arg, T>

    public companion object : ContainerFactory {

        private var instance: ContainerFactory = DefaultContainerFactory()

        override fun <T> createSubject(
            cacheTimeoutMillis: Long?,
            coroutineScopeFactory: CoroutineScopeFactory?,
            transformation: ContainerTransformation<T>?,
            valueLoader: ValueLoader<T>
        ): LazyFlowSubject<T> {
            return instance.createSubject(cacheTimeoutMillis, coroutineScopeFactory, transformation, valueLoader)
        }

        override fun <Arg, T> createCache(
            cacheTimeoutMillis: Long?,
            coroutineScopeFactory: CoroutineScopeFactory?,
            transformation: ContainerTransformation<T>?,
            valueLoader: CacheValueLoader<Arg, T>
        ): LazyCache<Arg, T> {
            return instance.createCache(cacheTimeoutMillis, coroutineScopeFactory, transformation, valueLoader)
        }

        /**
         * Replace the default factory by the custom one.
         */
        public fun setFactory(factory: ContainerFactory) {
            instance = factory
        }

        /**
         * Restore the default factory.
         */
        public fun resetFactory() {
            instance = DefaultContainerFactory()
        }

    }

}
