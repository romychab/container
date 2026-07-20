package com.elveum.store.stores.keyed

import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import kotlinx.coroutines.flow.StateFlow

/**
 * A [KeyedStore] whose value for each key is additionally parameterized by a query
 * (search text, filter, sorting, etc.).
 *
 * Each key keeps its own current query. Submitting a new query for a key re-fetches
 * that key's value while leaving the queries of other keys untouched.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of the value cached per key.
 */
public interface KeyedQueryStore<Key : Any, Q : Any, T : Any> : KeyedStore<Key, T> {

    /**
     * Observe the current query associated with the given [key].
     *
     * @param key the key whose query should be observed.
     * @return a [StateFlow] emitting the current and future queries for [key].
     */
    public fun observeQueryFlow(key: Key): StateFlow<Q>

    /**
     * Submit a new [query] for the given [key], triggering a reload of that key's value.
     *
     * This is a suspending call that returns once the reload triggered by the new query
     * has completed. Use [submitQueryAsync] for a fire-and-forget variant.
     *
     * **Requires an active observer for [key].** A key retains its query only while it has
     * at least one active observer. Submitting a query for a key that is not currently
     * observed has no effect; start observing [key] before submitting a query for it.
     *
     * @param key the key whose query should be updated.
     * @param query the new query value.
     * @param metadata custom metadata values merged into the emitted result.
     */
    public suspend fun submitQuery(key: Key, query: Q, metadata: ContainerMetadata = EmptyMetadata)

    /**
     * The same as [submitQuery], but returns immediately without waiting for the
     * reload to complete.
     *
     * @param key the key whose query should be updated.
     * @param query the new query value.
     * @param metadata custom metadata values merged into the emitted result.
     */
    public fun submitQueryAsync(key: Key, query: Q, metadata: ContainerMetadata = EmptyMetadata)
}
