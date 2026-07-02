package com.elveum.container.cache

import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.subject.LazyFlowSubject

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
     * @param arg the argument identifying the cache entry to create a subject for
     * @param coroutineScopeFactory factory used to create coroutine scopes for loading
     * @param cacheTimeoutMillis how much time loaded values remain cached when there are no collectors
     * @return the created [LazyFlowSubject] instance
     */
    public fun create(
        arg: Arg,
        coroutineScopeFactory: CoroutineScopeFactory,
        cacheTimeoutMillis: Long,
    ): LazyFlowSubject<T>
}
