package com.elveum.container.subject.paging.internal

import com.elveum.container.Emitter
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.paging.PageEmitter
import com.elveum.container.subject.paging.PageLoader
import com.elveum.container.subject.paging.PageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.supervisorScope

internal class PageLoaderImpl<Key, T>(
    private val initialKey: Key,
    private val block: suspend PageEmitter<Key, T>.(Key) -> Unit,
    private val factory: Factory<Key, T> = Factory(block),
) : PageLoader<Key, T> {

    override val nextPageState: MutableStateFlow<PageState> = MutableStateFlow(PageState.Idle)

    private var launcher: PageLoadTaskLauncher<Key, T>? = null

    override fun onItemRendered(index: Int) {
        val launcher = this.launcher
        val nextKey = launcher?.findNextKeyForIndex(index)
        if (nextKey != null) {
            launcher.launch(nextKey)
        }
    }

    override suspend fun Emitter<List<T>>.invoke(): Unit = Unit

    override suspend fun StatefulEmitter<List<T>>.statefulInvoke() {
        try {
            supervisorScope {
                val state = factory.createState(
                    emitter = this@statefulInvoke,
                    onNextPageStateChanged = { pageState ->
                        nextPageState.value = pageState
                    }
                )
                val launcher = factory.createLauncher(
                    state = state,
                    coroutineScope = this@supervisorScope,
                    emitter = this@statefulInvoke,
                ).also {
                    this@PageLoaderImpl.launcher = it
                }

                launcher.launch(initialKey)

                state.await()
            }
        } finally {
            nextPageState.update { PageState.Idle }
            launcher = null
        }
    }

    class Factory<Key, T>(
        private val block: suspend PageEmitter<Key, T>.(Key) -> Unit,
    ) {

        fun createState(
            emitter: StatefulEmitter<List<T>>,
            onNextPageStateChanged: (PageState) -> Unit,
        ): PageLoaderState<Key, T> {
            val store = PageLoaderRecordStore<Key, T>()
            val pageResultsEmitter = PageResultsEmitter(
                store = store,
                emitter = emitter,
                onNextPageStateChanged = onNextPageStateChanged,
            )
            return PageLoaderState(pageResultsEmitter, store)
        }

        fun createLauncher(
            state: PageLoaderState<Key, T>,
            coroutineScope: CoroutineScope,
            emitter: StatefulEmitter<List<T>>,
        ): PageLoadTaskLauncher<Key, T> {
            return PageLoadTaskLauncher(state, coroutineScope, emitter, block)
        }

    }

}
