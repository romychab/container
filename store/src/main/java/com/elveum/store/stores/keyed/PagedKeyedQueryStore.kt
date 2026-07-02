package com.elveum.store.stores.keyed

/**
 * A [PagedKeyedStore] whose paginated value for each key is additionally parameterized
 * by a query (search text, filter, sorting, etc.).
 *
 * Combines per-key pagination (see [PagedKeyedStore]) with per-key queries (see
 * [KeyedQueryStore]). Submitting a new query for a key resets that key's pagination and
 * reloads it from the first page.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedQueryStore<Key : Any, Q : Any, T : Any> :
    KeyedQueryStore<Key, Q, List<T>>,
    PagedKeyedStore<Key, T>
