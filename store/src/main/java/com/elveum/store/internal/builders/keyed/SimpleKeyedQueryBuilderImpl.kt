package com.elveum.store.internal.builders.keyed

import com.elveum.container.Emitter
import com.elveum.store.builders.SimpleKeyedQueryBuilder
import com.elveum.store.builders.SimpleKeyedQueryReactiveBuilder
import com.elveum.store.builders.SimpleKeyedQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleKeyedQuerySuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQueryContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.common.CoreFetcher
import com.elveum.store.stores.keyed.KeyedQueryStore
import com.elveum.store.stores.simple.SimpleStore

internal class SimpleKeyedQueryBuilderImpl<Key : Any, Q : Any, T : Any>(
    val initialQuery: Q,
    val queryDebounceMillis: Long,
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedQueryBuilder<Key, Q, T>> = BaseBuilderImpl(config),
) : SimpleKeyedQueryBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedQueryBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun disableFetcher(): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T> {
        return SimpleKeyedQueryReactiveNoFetcherBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun addSuspendingLocalStorage(): SimpleKeyedQuerySuspendingBuilder<Key, Q, T> {
        return SimpleKeyedQuerySuspendingBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun addReactiveLocalStorage(): SimpleKeyedQueryReactiveBuilder<Key, Q, T> {
        return SimpleKeyedQueryReactiveBuilderImpl(initialQuery, queryDebounceMillis, config)
    }


    override fun build(contract: SimpleKeyedQueryContract<Key, Q, T>): KeyedQueryStore<Key, Q, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Key, Q) -> T): KeyedQueryStore<Key, Q, T> {
        return KeyedQueryStoreImpl(
            fetcher = onFetch,
            config = config,
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
        )
    }

    override fun buildCustom(loader: suspend Emitter<T>.(Key, Q) -> Unit): KeyedQueryStore<Key, Q, T> {
        return KeyedQueryStoreImpl(
            fetcher = CoreFetcher.Custom { key, query -> loader(key, query) },
            config = config,
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
        )
    }
}
