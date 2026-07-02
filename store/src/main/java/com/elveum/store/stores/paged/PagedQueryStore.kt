package com.elveum.store.stores.paged

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.BasePagedStore
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.WithQuery
import com.elveum.store.stores.base.WithStoreLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A store loading chunks of data using the specified input query. All chunks are merged
 * into the one final list that can be displayed in Lazy-containers with endless scrolling.
 */
public interface PagedQueryStore<Q : Any, T : Any> :
    BasePagedStore<T>,
    WithQuery<Q>,
    WithStoreLifecycleOwner<PagedQueryStore<Q, T>> {

    /**
     * The current query used to fetch the data.
     */
    override val queryFlow: StateFlow<Q>

    /**
     * Submit a query and wait for finishing. The submitted query can be
     * canceled by a new query.
     */
    override suspend fun submitQuery(query: Q)

    /**
     * The same as [submitQuery], but without waiting for results.
     */
    override fun submitQueryAsync(query: Q)

    /**
     * Notify the store that the item under [index] has been rendered to the user.
     *
     * The store can trigger the load of the next page when the rendered item is
     * located close enough to the next page. The fetch distance is determined by store
     * builder.
     */
    override fun onItemRendered(index: Int)

    /**
     * Observe data managed by the store. Optionally, you can specify an additional
     * load [request] to specify how data must be loaded. If [request] is `null`,
     * the store's configured default load request is used instead
     * (see `BaseBuilder.setLoadRequest`).
     */
    override fun observe(request: LoadRequest?): Flow<StoreResult<List<T>>>

    /**
     * Force a reload of the data managed by the store.
     *
     * Invalidation intentionally does not accept a [LoadRequest]: the store may have
     * several observers, each subscribed with its own request via [observe] (or the
     * store's configured default). Every observer keeps receiving data according to the
     * request it subscribed with. Use [invalidateAsync] for a fire-and-forget variant.
     */
    override suspend fun invalidate()

    /**
     * The same as [invalidate], but without waiting for invalidation results.
     */
    override fun invalidateAsync()

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * See [BaseStore.optimisticUpdate] for more details.
     *
     * See also [update] extension.
     */
    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<List<T>>.(List<T>) -> Unit)

    /**
     * Register a [block] of code which is executed when the store becomes active (while at
     * least one observer start listening the store, and until cache is released).
     *
     * See [WithStoreLifecycleOwner.whenActive] for more details.
     */
    override fun whenActive(block: suspend PagedQueryStore<Q, T>.() -> Unit): PagedQueryStore<Q, T>
}
