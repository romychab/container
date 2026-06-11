package com.elveum.store

import com.elveum.store.builders.PagedBuilder
import com.elveum.store.builders.KeyedBuilder
import com.elveum.store.builders.SimpleBuilder
import com.elveum.store.internal.StoreFactoryImpl

/**
 * A factory that provides 3 builders for creating Store instances:
 *
 * - `simpleStoreBuilder()` builds a store for fetching and caching
 *   data without keys. For example: user profile, non-paged lists,
 *   shopping cart, etc.
 *
 * - `keyedStoreBuilder()` acts like a map of stores, where each store has a
 *   key with auto-managed lifecycle determined by observers. Usually such
 *   stores can be used for item details fetched by unique identifiers:
 *   friend profiles, product details, movie details, etc.
 *
 * - `pagedStoreBuilder()` builds a store which data should be rendered
 *   as infinite scrollable list, loaded by chunks. For example: news feed,
 *   reels, posts, gallery, etc.
 */
public interface StoreFactory {

    /**
     * Build a simple store for fetching and caching data without keys. The
     * builder can configure:
     *
     * - in-memory cache timeout
     * - coroutine scope (e.g. dispatcher)
     * - suspending local storage (e.g. DAO class with suspend functions)
     * - reactive local storage (e.g. Data Store with Flow)
     * - query type
     *
     * Depending on configured values, the `build` method accepts different contracts
     * and returns pre-configured store with different capabilities.
     */
    public fun <T : Any> simpleStoreBuilder(): SimpleBuilder<T>

    /**
     * Build a map of stores where each key-store pair has auto-managed lifecycle
     * determined by observers. The key and corresponding loaded value is cached
     * while observers are interested in its value (plus configurable in-memory
     * cache timeout). This store builder can configure:
     *
     * - key type (e.g. user ID, movie ID)
     * - in-memory cache timeout
     * - coroutine context (e.g. dispatcher)
     * - suspending local storage (e.g. DAO class with suspend functions)
     * - reactive local storage (e.g. Data Store with Flow)
     *
     * Depending on configured values, the `build` method accepts different contracts
     * and returns pre-configured keyed store with different capabilities.
     */
    public fun <Key : Any, T : Any> keyedStoreBuilder(): KeyedBuilder<Key, T>

    /**
     * Build a store which data should be rendered as infinite scrollable list,
     * but loaded by chunks / pages. This store builder can configure:
     *
     * - page key type (e.g. page index, cursor, limit/offset)
     * - initial page key
     * - fetch distance
     * - entity ID (used for deduplication)
     * - in-memory cache timeout
     * - coroutine scope (e.g. dispatcher)
     * - suspending local storage (e.g. DAO class with suspend functions)
     * - reactive local storage (e.g. Data Store with Flow)
     * - query type
     *
     * Depending on configured values, the `build` method accepts different contracts
     * and returns pre-configured paged store with different capabilities.
     */
    public fun <PageKey : Any, T : Any> pagedStoreBuilder(
        initialKey: PageKey,
        itemId: (T) -> Any,
    ): PagedBuilder<PageKey, T>

    /**
     * Default implementation that creates new stores without having a separate factory instance.
     *
     * ```
     * val store = StoreFactory.keyedStoreBuilder<Long, Product>()
     *     .setInMemoryCacheTimeout(60.seconds)
     *     .addSuspendingLocalStorage()
     *     .build(...)
     * ```
     *
     * It can also be used as a value in constructor args for better testability:
     *
     * ```
     * @Module
     * @InstallIn
     * object StoreFactoryModule {
     *     @Provides
     *     fun provideStoreFactory(): StoreFactory = StoreFactory
     * }
     *
     * @Singleton
     * class MyRepository @Inject constructor(
     *     private val storeFactory: StoreFactory
     * )
     * ```
     *
     */
    public companion object : StoreFactory {

        override fun <Key : Any, T : Any> keyedStoreBuilder(): KeyedBuilder<Key, T> {
            return StoreFactoryImpl.keyedStoreBuilder()
        }

        override fun <PageKey : Any, T : Any> pagedStoreBuilder(
            initialKey: PageKey,
            itemId: (T) -> Any
        ): PagedBuilder<PageKey, T> {
            return StoreFactoryImpl.pagedStoreBuilder(initialKey, itemId)
        }

        override fun <T : Any> simpleStoreBuilder(): SimpleBuilder<T> {
            return StoreFactoryImpl.simpleStoreBuilder()
        }

    }
}
