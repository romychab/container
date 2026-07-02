package com.elveum.store

import com.elveum.store.builders.PagedBuilder
import com.elveum.store.builders.SimpleBuilder
import com.elveum.store.internal.StoreFactoryImpl

/**
 * A factory that provides builders for creating Store instances:
 *
 * - `simpleStoreBuilder()` builds a store for fetching and caching
 *   data without keys. For example: user profile, non-paged lists,
 *   shopping cart, etc.
 *
 * - `pagedStoreBuilder()` builds a store which data should be rendered
 *   as infinite scrollable list, loaded by chunks. For example: news feed,
 *   reels, posts, gallery, etc.
 *
 * Keyed stores - which act like a map of stores, where each key has an
 * auto-managed lifecycle determined by its observers (friend profiles,
 * product details, movie details, etc.) - are not created directly by this
 * factory. Instead, call `withKeys<Key>()` on a simple or paged builder, e.g.
 * `simpleStoreBuilder<Product>().withKeys<Long>()`.
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
     * val store = StoreFactory.simpleStoreBuilder<Product>()
     *     .withKeys<Long>()
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
