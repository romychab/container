package com.elveum.store.internal.builders.simple

import com.elveum.container.Emitter
import com.elveum.store.builders.SimpleKeyedQueryBuilder
import com.elveum.store.builders.SimpleQueryBuilder
import com.elveum.store.builders.SimpleQueryReactiveBuilder
import com.elveum.store.builders.SimpleQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleQuerySuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedQueryBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleQueryStore
import com.elveum.store.internal.stores.common.CoreFetcher
import com.elveum.store.stores.simple.SimpleQueryStore
import com.elveum.store.stores.simple.SimpleStore

internal class SimpleQueryBuilderImpl<Q : Any, T : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleQueryBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleQueryBuilder<Q, T>, BaseBuilder<SimpleQueryBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): SimpleKeyedQueryBuilder<Key, Q, T> {
        return SimpleKeyedQueryBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun addSuspendingLocalStorage(): SimpleQuerySuspendingBuilder<Q, T> {
        return SimpleQuerySuspendingBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun addReactiveLocalStorage(): SimpleQueryReactiveBuilder<Q, T> {
        return SimpleQueryReactiveBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun disableFetcher(): SimpleQueryReactiveNoFetcherBuilder<Q, T> {
        return SimpleQueryReactiveNoFetcherBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun build(contract: SimpleQueryContract<Q, T>): SimpleQueryStore<Q, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Q) -> T): SimpleQueryStore<Q, T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query -> onFetch(query) },
        ).asSimpleQueryStore()
    }

    override fun buildCustom(loader: suspend Emitter<T>.(Q) -> Unit): SimpleQueryStore<Q, T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = CoreFetcher.Custom { _, query -> loader(query) },
        ).asSimpleQueryStore()

    }
}
