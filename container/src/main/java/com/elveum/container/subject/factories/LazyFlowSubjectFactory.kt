package com.elveum.container.subject.factories

import com.elveum.container.subject.FlowSubjects
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.LazyFlowSubjectImpl
import com.elveum.container.subject.lazy.LoadTaskManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

public interface LazyFlowSubjectFactory {

    /**
     * Create a [LazyFlowSubject] instance. The factory
     * can be assigned via [FlowSubjects.setDefaultConfiguration] so
     * [LazyFlowSubject.create] will use it as a default factory.
     */
    public fun <T> create(
        cacheTimeoutMillis: Long,
        loadingDispatcher: CoroutineDispatcher,
    ): LazyFlowSubject<T>

}

/**
 * Create a new instance of [LazyFlowSubject].
 * Usually you don't need to call this method in production code. Use [LazyFlowSubject.create] instead.
 *
 * This method can be used in custom factories assigned via [FlowSubjects.setDefaultConfiguration]
 * if you want to use TestScope in [LazyFlowSubject] instances.
 */
public fun <T> createLazyFlowSubject(
    loadingDispatcher: CoroutineDispatcher,
    cacheTimeoutMillis: Long,
    loadingScopeFactory: LoadingScopeFactory? = null,
): LazyFlowSubject<T> {
    return createLazyFlowSubject(loadingDispatcher, cacheTimeoutMillis, loadingScopeFactory, LoadTaskManager())
}

internal fun <T> createLazyFlowSubject(
    loadingDispatcher: CoroutineDispatcher,
    cacheTimeoutMillis: Long,
    loadingScopeFactory: LoadingScopeFactory?,
    loadTaskManager: LoadTaskManager<T>,
): LazyFlowSubject<T> = LazyFlowSubjectImpl(
    loadingScopeFactory = loadingScopeFactory ?:
    LoadingScopeFactory {
        CoroutineScope(SupervisorJob() + it)
    },
    loadingDispatcher = loadingDispatcher,
    cacheTimeoutMillis = cacheTimeoutMillis,
    loadTaskManager = loadTaskManager,
)
