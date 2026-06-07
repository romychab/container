package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.paging.PageEmitter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PageEmitterImpl<Key, T>(
    private val context: PageContext<Key, T>,
    private val originEmitter: StatefulEmitter<List<T>>,
) : PageEmitter<Key, T> {

    private val mutex = Mutex()
    private val emittedKeys = mutableSetOf<Key>()

    @Volatile
    var isPageEmitted = false
        private set

    override suspend fun emitPage(list: List<T>) {
        isPageEmitted = true
        context.onPageDataLoaded(list)
    }

    override suspend fun emitNextKey(key: Key) {
        val shouldEmit = mutex.withLock {
            if (!emittedKeys.contains(key)) {
                emittedKeys.add(key)
                true
            } else {
                false
            }
        }
        if (shouldEmit) {
            context.scheduleNextKey(key)
        }
    }

    override suspend fun <R> dependsOnFlow(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<R>
    ): R {
        return originEmitter.dependsOnFlow(key = key, keys = keys, flow = flow)
    }

    override suspend fun <R> dependsOnContainerFlow(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<Container<R>>
    ): R {
        return originEmitter.dependsOnContainerFlow(key = key, keys = keys, flow = flow)
    }

}
