package com.elveum.container.subject.factories

import com.elveum.container.subject.Creator
import com.elveum.container.subject.FlowSubjects
import com.elveum.container.subject.LazyFlowSubject
import kotlinx.coroutines.CoroutineDispatcher

interface LazyFlowSubjectFactory {

    /**
     * Create a [LazyFlowSubject] instance. The factory
     * can be assigned via [FlowSubjects.setDefaultConfiguration] so
     * [LazyFlowSubject.create] will use it as a default factory.
     */
    fun <T> create(
        cacheTimeoutMillis: Long,
        loadingDispatcher: CoroutineDispatcher,
        creator: Creator,
    ): LazyFlowSubject<T>

}