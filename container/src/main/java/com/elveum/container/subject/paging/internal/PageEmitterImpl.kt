package com.elveum.container.subject.paging.internal

import com.elveum.container.Emitter
import com.elveum.container.FlowComposer
import com.elveum.container.subject.paging.PageEmitter
import java.util.concurrent.atomic.AtomicBoolean

internal class PageEmitterImpl<Key, T>(
    private val key: Key,
    private val state: PageLoaderState<Key, T>,
    private val emitter: Emitter<List<T>>,
    private val launcher: PageLoadTaskLauncher<Key, T>,
) : PageEmitter<Key, T>, FlowComposer by emitter {

    var emitPageCalled = false
        private set

    private val emitNextKeyCalled = AtomicBoolean(false)

    override suspend fun emitPage(list: List<T>) {
        emitPageCalled = true
        state.onKeyLoaded(key, list)
    }

    override suspend fun emitNextKey(key: Key) {
        if (emitNextKeyCalled.compareAndSet(false, true)) {
            if (state.isImmediateLaunchScheduled()) {
                launcher.launch(key)
            } else {
                state.registerKey(key)
            }
        } else {
            throw IllegalStateException(
                "emitNextKey() can be called either 0 or 1 time. " +
                        "Multiple calls of emitNextKey() are not allowed."
            )
        }
    }

}
