package com.elveum.container.factory

import com.elveum.container.subject.LazyFlowSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

public fun interface CoroutineScopeFactory {

    /**
     * Create a coroutine scope for loading data by [LazyFlowSubject].
     */
    public fun createScope(): CoroutineScope

    public companion object : CoroutineScopeFactory {
        override fun createScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
    }
}
