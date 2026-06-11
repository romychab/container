package com.elveum.store.stores.base

import com.elveum.store.load.LoadRequest
import kotlinx.coroutines.flow.StateFlow

/**
 * Base query support for all stores that can fetch/load data by query.
 */
public interface WithQuery<Q> {

    /**
     * The current query used to fetch the data.
     */
    public val queryFlow: StateFlow<Q>

    /**
     * Submit a query and wait for finishing. The submitted query can be
     * canceled by a new query.
     */
    public suspend fun submitQuery(
        query: Q,
        loadRequest: LoadRequest = LoadRequest.Silent,
    )

    /**
     * The same as [submitQuery], but without waiting for results.
     */
    public fun submitQueryAsync(
        query: Q,
        loadRequest: LoadRequest = LoadRequest.Silent,
    )

}
