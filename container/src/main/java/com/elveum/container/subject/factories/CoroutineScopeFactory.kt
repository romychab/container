package com.elveum.container.subject.factories

import com.elveum.container.subject.LazyFlowSubject
import kotlinx.coroutines.CoroutineScope

public fun interface CoroutineScopeFactory {

    /**
     * Create a coroutine scope for loading data by [LazyFlowSubject].
     */
    public fun createScope(): CoroutineScope

}