package com.elveum.store.builders

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.contracts.PagedQueryContract
import com.elveum.store.contracts.PagedQuerySuspendingContract
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedStore

/**
 * Builder for a remote-only [PagedStore] whose query is driven by an external
 * [kotlinx.coroutines.flow.Flow].
 *
 * Created via `PagedBuilder.withQuery { flow }`. The store follows the supplied query flow -
 * a new query resets pagination and reloads from the first page - and exposes no query API of
 * its own.
 *
 * @param Q the type representing the query.
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in the paged list.
 */
public interface PagedExternalQueryBuilder<Q : Any, PageKey : Any, T : Any> :
    BasePagedBuilder<PagedExternalQueryBuilder<Q, PageKey, T>> {

    /**
     * Transitions the builder into a keyed variant. The configured external query flow is shared
     * by every key (each key follows the same query stream). For per-key query flows, call
     * `withKeys()` before `withQuery { key -> ... }` instead.
     *
     * @param Key the type of the keys managed by the resulting store.
     */
    public fun <Key : Any> withKeys(): PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot) local storage
     * layer, preserving the configured external query flow.
     */
    public fun addSuspendingLocalStorage(): PagedExternalQuerySuspendingBuilder<Q, PageKey, T>

    /**
     * Builds a [PagedStore] using the provided [PagedQueryContract] implementation.
     */
    public fun build(contract: PagedQueryContract<Q, PageKey, T>): PagedStore<T>

    /**
     * Builds a [PagedStore] using a single lambda that fetches a page for the current query and
     * a page key.
     */
    public fun build(onFetch: suspend (Q, PageKey) -> PagedList<PageKey, T>): PagedStore<T>

    /**
     * Builds a [PagedStore] with a custom loader that emits pages manually through a
     * [PageEmitter], receiving the current query and page key.
     */
    public fun buildCustom(loader: suspend PageEmitter<PageKey, T>.(Q, PageKey) -> Unit): PagedStore<T>
}

/**
 * Builder for a [PagedStore] backed by suspending local storage whose query is driven by an
 * external [kotlinx.coroutines.flow.Flow]. Created via `PagedSuspendingBuilder.withQuery { flow }`.
 *
 * @param Q the type representing the query.
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in the paged list.
 */
public interface PagedExternalQuerySuspendingBuilder<Q : Any, PageKey : Any, T : Any> :
    BasePagedBuilder<PagedExternalQuerySuspendingBuilder<Q, PageKey, T>> {

    /**
     * Transitions the builder into a keyed variant with suspending local storage. The configured
     * external query flow is shared by every key.
     *
     * @param Key the type of the keys managed by the resulting store.
     */
    public fun <Key : Any> withKeys(): PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>

    /**
     * Builds a [PagedStore] using the provided [PagedQuerySuspendingContract] implementation.
     */
    public fun build(contract: PagedQuerySuspendingContract<Q, PageKey, T>): PagedStore<T>

    /**
     * Builds a [PagedStore] using individual lambdas for remote page fetch and suspending local
     * storage, each receiving the current query and page key.
     */
    public fun build(
        onFetch: suspend (Q, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Q, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Q, PageKey) -> PagedList<PageKey, T>?,
    ): PagedStore<T>
}
