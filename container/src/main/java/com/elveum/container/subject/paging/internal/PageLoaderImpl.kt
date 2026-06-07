package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.paging.PageLoader
import com.elveum.container.subject.paging.PageState
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.supervisorScope

internal class PageLoaderImpl<Key, T>(
    private val config: PageLoaderConfig<Key, T>,
) : PageLoader<Key, T> {

    override val nextPageState = MutableStateFlow<PageState>(PageState.Idle)

    @Volatile
    private var loadSession: PageLoadSession<Key, T>? = null

    override suspend fun StatefulEmitter<List<T>>.statefulInvoke() = supervisorScope {
        val loadSession = PageLoadSession(
            coroutineScope = this,
            originEmitter = this@statefulInvoke,
            config = config,
            onNextPageStateChanged = { nextPageState.value = it }
        ).also { this@PageLoaderImpl.loadSession = it }

        try {
            loadSession.startLoading(pageIndex = 0, pageKey = config.initialKey)
            loadSession.await()
            emitCompletedState()
        } catch (e: Exception) {
            ensureActive()
            emitFailureState(e)
        } finally {
            this@PageLoaderImpl.loadSession = null
        }
    }

    override suspend fun Emitter<List<T>>.invoke() = Unit

    override fun intercept(container: Container<List<T>>): Container<List<T>> {
        return loadSession?.intercept(container) ?: container
    }

    override fun onItemRendered(index: Int) {
        loadSession?.onItemRendered(index)
    }

}
