package com.elveum.store.stores.keyed

/**
 * A [KeyedStore] whose value for each key is a paginated [List] loaded page by page.
 *
 * Each key manages its own pagination independently: the pages loaded for one key do
 * not affect any other key. Call [onItemRendered] as list items for a given key become
 * visible so the store can load the next page automatically.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedStore<Key : Any, T : Any> : KeyedStore<Key, List<T>> {

    /**
     * Notify the store that the item at the given [index] of the list associated with
     * [key] has been rendered. When the rendered item is close enough to the end of the
     * loaded list (within the configured fetch distance), the store loads the next page
     * for that key.
     *
     * @param key the key whose list is being scrolled.
     * @param index the index of the rendered item within that key's list.
     */
    public fun onItemRendered(key: Key, index: Int)
}
