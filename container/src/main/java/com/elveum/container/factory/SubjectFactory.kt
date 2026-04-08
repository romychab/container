package com.elveum.container.factory

import com.elveum.container.cache.CacheValueLoader
import com.elveum.container.cache.LazyCache
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation

public const val DEFAULT_CACHE_TIMEOUT_MILLIS: Long = 1000L
public const val DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS: Long = 50L

/**
 * Factory that can create [LazyFlowSubject] and [LazyCache] instances.
 */
public interface SubjectFactory {

    /**
     * Create a new [LazyFlowSubject] instance which loads
     * data by using the specified [valueLoader].
     */
    public fun <T> createSubject(
        cacheTimeoutMillis: Long? = null,
        reloadDependenciesPeriodMillis: Long? = null,
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
        reloadDependenciesPeriodMillis: Long? = null,
        coroutineScopeFactory: CoroutineScopeFactory? = null,
        transformation: ContainerTransformation<T>? = null,
        valueLoader: CacheValueLoader<Arg, T>,
    ): LazyCache<Arg, T>

    public companion object : SubjectFactory {

        @Volatile
        private var instance: SubjectFactory = DefaultSubjectFactory()

        override fun <T> createSubject(
            cacheTimeoutMillis: Long?,
            reloadDependenciesPeriodMillis: Long?,
            coroutineScopeFactory: CoroutineScopeFactory?,
            transformation: ContainerTransformation<T>?,
            valueLoader: ValueLoader<T>
        ): LazyFlowSubject<T> {
            return instance.createSubject(cacheTimeoutMillis, reloadDependenciesPeriodMillis,
                coroutineScopeFactory, transformation, valueLoader)
        }

        override fun <Arg, T> createCache(
            cacheTimeoutMillis: Long?,
            reloadDependenciesPeriodMillis: Long?,
            coroutineScopeFactory: CoroutineScopeFactory?,
            transformation: ContainerTransformation<T>?,
            valueLoader: CacheValueLoader<Arg, T>
        ): LazyCache<Arg, T> {
            return instance.createCache(cacheTimeoutMillis, reloadDependenciesPeriodMillis,
                coroutineScopeFactory, transformation, valueLoader)
        }

        /**
         * Replace the default factory by the custom one.
         */
        public fun setFactory(factory: SubjectFactory) {
            instance = factory
        }

        /**
         * Restore the default factory.
         */
        public fun resetFactory() {
            instance = DefaultSubjectFactory()
        }

    }

}
