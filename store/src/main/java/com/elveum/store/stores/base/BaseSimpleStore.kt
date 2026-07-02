package com.elveum.store.stores.base

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for non-keyed stores that expose a single observable value,
 * adding [observe] on top of the invalidation and update operations of [BaseStore].
 *
 * @param T the type of data held by the store.
 */
public interface BaseSimpleStore<T : Any> : BaseStore<T> {

    /**
     * Observe the data managed by the store.
     *
     * Loading starts lazily when the first observer subscribes; subsequent observers
     * share the same in-memory cache.
     *
     * @param request the [LoadRequest] controlling how this observer loads and shows
     *   data (fresh, offline, keeping content on reload, etc.). When `null`, the
     *   store's default request configured via
     *   [com.elveum.store.builders.base.BaseBuilder.setLoadRequest] is used.
     * @return a [Flow] emitting [StoreResult]s.
     */
    public fun observe(request: LoadRequest? = null): Flow<StoreResult<T>>

}
