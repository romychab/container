package com.elveum.container.subject.paging.internal

import com.elveum.container.Emitter
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.paging.PageEmitter
import com.elveum.container.subject.paging.PageLoader
import com.elveum.container.subject.paging.PageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.supervisorScope

internal class PageLoaderImpl<Key, T>(
    private val initialKey: Key,
    private val block: suspend PageEmitter<Key, T>.(Key) -> Unit,
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
                val state = PageLoaderState<Key, T>(
                    emitter = this@statefulInvoke,
                    onNextPageStateChanged = { pageState ->
                        nextPageState.value = pageState
                    }
                )
                val launcher = PageLoadTaskLauncher(
                    state = state,
                    coroutineScope = this@supervisorScope,
                    emitter = this@statefulInvoke,
                    block = block,
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

}
