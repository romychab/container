package com.elveum.container.factory

import com.elveum.container.ContainerMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.cache.CacheValueLoader
import com.elveum.container.cache.LazyCache
import com.elveum.container.cache.LazyFlowSubjectFactory
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation
import com.elveum.container.subject.transformation.LoaderDecorator

public open class DefaultSubjectFactory(
    private val cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    private val reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS,
    private val coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
    private val transformationFactory: TransformationFactory = TransformationFactory,
    private val loaderDecorator: LoaderDecorator = LoaderDecorator,
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
            loaderDecorator = loaderDecorator,
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
            loaderDecorator = loaderDecorator,
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
            cacheTimeoutMillis = cacheTimeoutMillis ?: this.cacheTimeoutMillis,
            coroutineScopeFactory = coroutineScopeFactory ?: this.coroutineScopeFactory,
            reloadDependenciesPeriodMillis = reloadDependenciesPeriodMillis,
            transformation = transformationFactory.create(),
            loaderDecorator = loaderDecorator,
            factory = factory,
        )
    }

}
