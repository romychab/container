package com.elveum.store.internal.builders

import com.elveum.container.factory.CoroutineScopeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal open class SharedConfig {
    var coroutineScopeFactory: CoroutineScopeFactory? = null
    var inMemoryCacheTimeout: Duration = 5.seconds
    var coroutineContext: CoroutineContext = EmptyCoroutineContext

    internal fun buildCoroutineScopeFactory() = coroutineScopeFactory
        ?: CoroutineScopeFactory { CoroutineScope(SupervisorJob() + coroutineContext) }
}
