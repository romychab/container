package com.elveum.store.internal.builders.keyed

import com.elveum.container.Emitter
import com.elveum.store.builders.SimpleKeyedBuilder
import com.elveum.store.builders.SimpleKeyedQueryBuilder
import com.elveum.store.builders.SimpleKeyedReactiveBuilder
import com.elveum.store.builders.SimpleKeyedReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleKeyedSuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.common.CoreFetcher
import com.elveum.store.stores.keyed.KeyedStore
import com.elveum.store.stores.simple.SimpleStore

internal class SimpleKeyedBuilderImpl<Key : Any, T : Any>(
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedBuilder<Key, T>> = BaseBuilderImpl(config),
) : SimpleKeyedBuilder<Key, T>, BaseBuilder<SimpleKeyedBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun disableFetcher(): SimpleKeyedReactiveNoFetcherBuilder<Key, T> {
        return SimpleKeyedReactiveNoFetcherBuilderImpl(config)
    }

    override fun addSuspendingLocalStorage(): SimpleKeyedSuspendingBuilder<Key, T> {
        return SimpleKeyedSuspendingBuilderImpl(config)
    }

    override fun addReactiveLocalStorage(): SimpleKeyedReactiveBuilder<Key, T> {
        return SimpleKeyedReactiveBuilderImpl(config)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): SimpleKeyedQueryBuilder<Key, Q, T> {
        return SimpleKeyedQueryBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun build(contract: SimpleKeyedContract<Key, T>): KeyedStore<Key, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Key) -> T): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            config = config,
            fetcher = { key, _ -> onFetch(key) },
            initialQuery = Unit,
        )
    }

    override fun buildCustom(loader: suspend Emitter<T>.(Key) -> Unit): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            config = config,
            fetcher = CoreFetcher.Custom { key, _ -> loader(key) },
            initialQuery = Unit,
        )
    }
}
