package com.elveum.store.internal.builders.keyed

import com.elveum.container.Emitter
import com.elveum.store.builders.SimpleKeyedExternalQueryBuilder
import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveBuilder
import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleKeyedExternalQuerySuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQueryContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.common.CoreFetcher
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow

internal class SimpleKeyedExternalQueryBuilderImpl<Key : Any, Q : Any, T : Any>(
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: (Key) -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedExternalQueryBuilder<Key, Q, T>> = BaseBuilderImpl(config),
) : SimpleKeyedExternalQueryBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedExternalQueryBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun addSuspendingLocalStorage(): SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T> {
        return SimpleKeyedExternalQuerySuspendingBuilderImpl(
            initialQueryProvider, queryDebounceMillis, queryFlow, config,
        )
    }

    override fun addReactiveLocalStorage(): SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryReactiveBuilderImpl(
            initialQueryProvider, queryDebounceMillis, queryFlow, config,
        )
    }

    override fun disableFetcher(): SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryReactiveNoFetcherBuilderImpl(
            initialQueryProvider, queryDebounceMillis, queryFlow, config,
        )
    }

    override fun build(contract: SimpleKeyedQueryContract<Key, Q, T>): KeyedStore<Key, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Key, Q) -> T): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            fetcher = onFetch,
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            externalQueryProvider = queryFlow,
        )
    }

    override fun buildCustom(loader: suspend Emitter<T>.(Key, Q) -> Unit): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            fetcher = CoreFetcher.Custom { key, query -> loader(key, query) },
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            externalQueryProvider = queryFlow,
        )
    }
}
