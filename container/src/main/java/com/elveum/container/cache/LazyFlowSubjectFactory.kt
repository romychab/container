package com.elveum.container.cache

import com.elveum.container.ContainerMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation
import com.elveum.container.subject.transformation.LoaderDecorator

/**
 * Creates a separate [LazyFlowSubject] for each argument used by a [LazyCache].
 *
 * This allows building caches whose entries are backed by fully customized
 * subjects, for example simple stores that don't have a local storage attached.
 *
 * @param Arg the type of the argument used to identify cached entries.
 * @param T the type of values held by the created subjects.
 * @see LazyCache.createFromFactory
 */
public fun interface LazyFlowSubjectFactory<Arg, T> {
    /**
     * Create a new [LazyFlowSubject] instance for the specified [arg].
     *
     * Use [LazyFlowSubjectCreationScope.newInstance] call to create a subject inheriting
     * configuration from the cache.
     *
     * @param arg the argument identifying the cache entry to create a subject for
     * @return the created [LazyFlowSubject] instance
     */
    public fun LazyFlowSubjectCreationScope<T>.create(arg: Arg): LazyFlowSubject<T>
}

public interface LazyFlowSubjectCreationScope<T> {
    public fun newInstance(
        cacheTimeoutMillis: Long? = null,
        reloadDependenciesPeriodMillis: Long? = null,
        coroutineScopeFactory: CoroutineScopeFactory? = null,
        transformation: ContainerTransformation<T>? = null,
        loaderDecorator: LoaderDecorator? = null,
        loadConfig: LoadConfig? = null,
        metadata: ContainerMetadata? = null,
        valueLoader: ValueLoader<T>,
    ): LazyFlowSubject<T>
}
