package com.elveum.container.subject.factories

import com.elveum.container.subject.LazyFlowSubject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

fun interface LoadingScopeFactory {

    /**
     * Create a coroutine scope for loading data by [LazyFlowSubject].
     */
    fun createScope(dispatcher: CoroutineDispatcher): CoroutineScope

}