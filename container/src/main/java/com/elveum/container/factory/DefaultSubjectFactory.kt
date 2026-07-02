package com.elveum.container.factory

import com.elveum.container.ContainerMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.cache.CacheValueLoader
import com.elveum.container.cache.LazyCache
import com.elveum.container.cache.LazyFlowSubjectFactory
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation

public open class DefaultSubjectFactory(
    private val cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    private val reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS,
    private val coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
    private val transformationFactory: TransformationFactory = TransformationFactory,
) : SubjectFactory {

    override fun <T> createSubject(
        cacheTimeoutMillis: Long?,
        reloadDependenciesPeriodMillis: Long?,
        coroutineScopeFactory: CoroutineScopeFactory?,
        transformation: ContainerTransformation<T>?,
        loadConfig: LoadConfig,
        metadata: ContainerMetadata,
        valueLoader: ValueLoader<T>
    ): LazyFlowSubject<T> {
        return LazyFlowSubject.create(
            valueLoader = valueLoader,
            reloadDependenciesPeriodMillis = reloadDependenciesPeriodMillis ?: this.reloadDependenciesPeriodMillis,
            cacheTimeoutMillis = cacheTimeoutMillis ?: this.cacheTimeoutMillis,
            coroutineScopeFactory = coroutineScopeFactory ?: this.coroutineScopeFactory,
            transformation = transformation ?: transformationFactory.create(),
            loadConfig = loadConfig,
            metadata = metadata,
        )
    }

    override fun <Arg, T> createCache(
        cacheTimeoutMillis: Long?,
        reloadDependenciesPeriodMillis: Long?,
        coroutineScopeFactory: CoroutineScopeFactory?,
        transformation: ContainerTransformation<T>?,
        loadConfig: LoadConfig,
        metadata: ContainerMetadata,
        valueLoader: CacheValueLoader<Arg, T>
    ): LazyCache<Arg, T> {
        return LazyCache.create(
            valueLoader = valueLoader,
            cacheTimeoutMillis = cacheTimeoutMillis ?: this.cacheTimeoutMillis,
            reloadDependenciesPeriodMillis = reloadDependenciesPeriodMillis ?: this.reloadDependenciesPeriodMillis,
            coroutineScopeFactory = coroutineScopeFactory ?: this.coroutineScopeFactory,
            transformation = transformation ?: transformationFactory.create(),
            loadConfig = loadConfig,
            metadata = metadata,
        )
    }

    override fun <Arg, T> createCacheFromFactory(
        cacheTimeoutMillis: Long?,
        coroutineScopeFactory: CoroutineScopeFactory?,
        factory: LazyFlowSubjectFactory<Arg, T>
    ): LazyCache<Arg, T> {
        return LazyCache.createFromFactory(
            cacheTimeoutMillis ?: this.cacheTimeoutMillis,
            coroutineScopeFactory ?: this.coroutineScopeFactory,
            factory,
        )
    }

}
