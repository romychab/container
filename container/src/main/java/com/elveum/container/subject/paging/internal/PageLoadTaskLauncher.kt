package com.elveum.container.subject.paging.internal

import com.elveum.container.Emitter
import com.elveum.container.subject.paging.PageEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class PageLoadTaskLauncher<Key, T>(
    private val state: PageLoaderState<Key, T>,
    private val coroutineScope: CoroutineScope,
    private val emitter: Emitter<List<T>>,
    private val block: suspend PageEmitter<Key, T>.(Key) -> Unit,
    private val emitterFactory: PageEmitterFactory<Key, T> = PageEmitterFactory(state, emitter)
) {

    fun launch(key: Key) {
        if (state.hasLaunchedKey(key)) return
        coroutineScope.launch {
            state.processKey(key, coroutineContext.job) {
                val pageEmitter = emitterFactory.create(key, this@PageLoadTaskLauncher)
                block(pageEmitter, key)
                if (!pageEmitter.emitPageCalled) {
                    throw IllegalStateException("emitPage() must be called at least once.")
                }
            }
        }
    }

    fun findNextKeyForIndex(index: Int): Key? {
        return state.findNextKeyForIndex(index)
    }

    class PageEmitterFactory<Key, T>(
        private val state: PageLoaderState<Key, T>,
        private val emitter: Emitter<List<T>>,
    ) {
        fun create(
            key: Key,
            launcher: PageLoadTaskLauncher<Key, T>,
        ): PageEmitterImpl<Key, T> = PageEmitterImpl(key, state, emitter, launcher)
    }

}
