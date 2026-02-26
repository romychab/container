package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

internal interface FlowDependencyStore {

    fun initialize(
        scope: CoroutineScope,
        recomposeFunction: RecomposeFunction,
    )

    fun begin(
        reloadDependencies: Boolean,
        loadConfig: LoadConfig,
    )

    suspend fun <R> dependsOn(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<Container<R>>,
    ): R

    fun end()

    fun shutdown()

    fun interface RecomposeFunction {
        fun execute(reloadDependencies: Boolean)
    }
}
