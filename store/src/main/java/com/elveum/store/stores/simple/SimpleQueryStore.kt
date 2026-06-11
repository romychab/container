package com.elveum.store.stores.simple

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.BaseSimpleStore
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.WithQuery
import com.elveum.store.stores.base.WithStoreLifecycleOwner
import com.elveum.store.stores.base.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The same as [SimpleStore] but with additional querying support.
 */
public interface SimpleQueryStore<Q : Any, T : Any> :
    BaseSimpleStore<T>,
    WithQuery<Q>,
    WithStoreLifecycleOwner<SimpleQueryStore<Q, T>> {

    /**
     * The current query used to fetch the data.
     */
    override val queryFlow: StateFlow<Q>

    /**
     * Submit a query and wait for finishing. The submitted query can be
     * discarded by a new query.
     */
    override suspend fun submitQuery(query: Q, loadRequest: LoadRequest)

    /**
     * The same as [submitQuery], but without waiting for results.
     */
    override fun submitQueryAsync(query: Q, loadRequest: LoadRequest)

    /**
     * Observe data managed by the store. Optionally, you can specify an additional
     * load [request] to specify how data must be loaded.
     */
    override fun observe(request: LoadRequest): Flow<StoreResult<T>>

    /**
     * Force reload data managed by the store. If the data is already loaded,
     * it will be reloaded according to policy defined by [request].
     */
    override suspend fun invalidate(request: LoadRequest)

    /**
     * The same as [invalidate], but without waiting for invalidation results.
     */
    override fun invalidateAsync(request: LoadRequest)

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * See [BaseStore.optimisticUpdate] for more details.
     *
     * See also: [update] extension.
     */
    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<T>.(T) -> Unit)

    /**
     * Register a [block] of code which is executed when the store becomes active (while at
     * least one observer start listening the store, and until cache is released).
     *
     * See [WithStoreLifecycleOwner.whenActive] for more details.
     */
    override fun whenActive(block: suspend SimpleQueryStore<Q, T>.() -> Unit): SimpleQueryStore<Q, T>

}
