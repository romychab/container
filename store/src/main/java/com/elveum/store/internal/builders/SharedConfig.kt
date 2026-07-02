package com.elveum.store.internal.builders

import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.store.load.LoadRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal open class SharedConfig {
    var coroutineScopeFactory: CoroutineScopeFactory? = null
    var inMemoryCacheTimeout: Duration = 5.seconds
    var coroutineContext: CoroutineContext = EmptyCoroutineContext

    var loadRequestFlow: Flow<LoadRequest> = flowOf(LoadRequest.Default)

    /**
     * Builds the [CoroutineScopeFactory] the store must use for its internal load tasks,
     * honouring an explicitly configured factory and otherwise falling back to a
     * supervised scope that runs on the configured [coroutineContext].
     */
    internal fun buildCoroutineScopeFactory(): CoroutineScopeFactory = coroutineScopeFactory
        ?: CoroutineScopeFactory { CoroutineScope(SupervisorJob() + coroutineContext) }

}
