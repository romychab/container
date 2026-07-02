package com.elveum.store.stores.base

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
     *
     * **Requires an active observer.** The query is applied to the in-memory cache, which
     * exists only while the store has at least one active observer. If the store is not
     * currently observed, the submitted query is not retained and this call has no effect;
     * start observing the store before submitting a query.
     */
    public suspend fun submitQuery(query: Q)

    /**
     * The same as [submitQuery], but without waiting for results.
     *
     * **Requires an active observer** - see [submitQuery]. When the store has no active
     * observer this call is a no-op.
     */
    public fun submitQueryAsync(query: Q)

}
