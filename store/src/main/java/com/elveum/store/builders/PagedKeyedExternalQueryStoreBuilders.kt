package com.elveum.store.builders

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.contracts.PagedKeyedQueryContract
import com.elveum.store.contracts.PagedKeyedQuerySuspendingContract
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList

/**
 * Builder for a remote-only keyed [PagedKeyedStore] whose per-key query is driven by an external
 * [kotlinx.coroutines.flow.Flow].
 *
 * Created via `PagedKeyedBuilder.withQuery { key -> flow }`. Each key follows its own query flow -
 * a new query for a key resets that key's pagination and reloads from the first page - and the
 * resulting store exposes no query API of its own.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedExternalQueryBuilder<Key : Any, Q : Any, PageKey : Any, T : Any> :
    BasePagedBuilder<PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T>> {

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot) local storage
     * layer, preserving the configured per-key external query flow.
     */
    public fun addSuspendingLocalStorage(): PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>

    /**
     * Builds a [PagedKeyedStore] using the provided [PagedKeyedQueryContract] implementation.
     */
    public fun build(contract: PagedKeyedQueryContract<Key, Q, PageKey, T>): PagedKeyedStore<Key, T>

    /**
     * Builds a [PagedKeyedStore] using a single lambda that fetches one page of a key's list for
     * a given query.
     */
    public fun build(
        onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
    ): PagedKeyedStore<Key, T>

    /**
     * Builds a [PagedKeyedStore] with a custom loader that emits pages manually through a
     * [PageEmitter], receiving the key, current query and page key.
     */
    public fun buildCustom(
        loader: suspend PageEmitter<PageKey, T>.(Key, Q, PageKey) -> Unit,
    ): PagedKeyedStore<Key, T>
}

/**
 * Builder for a keyed [PagedKeyedStore] backed by suspending local storage whose per-key query is
 * driven by an external [kotlinx.coroutines.flow.Flow]. Created via
 * `PagedKeyedSuspendingBuilder.withQuery { key -> flow }`.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedExternalQuerySuspendingBuilder<Key : Any, Q : Any, PageKey : Any, T : Any> :
    BasePagedBuilder<PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>> {

    /**
     * Builds a [PagedKeyedStore] using the provided [PagedKeyedQuerySuspendingContract]
     * implementation.
     */
    public fun build(contract: PagedKeyedQuerySuspendingContract<Key, Q, PageKey, T>): PagedKeyedStore<Key, T>

    /**
     * Builds a [PagedKeyedStore] using individual lambdas for page fetch and suspending local
     * storage, each receiving the key, current query and page key.
     */
    public fun build(
        onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Key, Q, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>?,
    ): PagedKeyedStore<Key, T>
}
