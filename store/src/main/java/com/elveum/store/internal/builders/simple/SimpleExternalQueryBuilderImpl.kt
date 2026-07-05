package com.elveum.store.internal.builders.simple

import com.elveum.container.Emitter
import com.elveum.store.builders.SimpleExternalQueryBuilder
import com.elveum.store.builders.SimpleExternalQueryReactiveBuilder
import com.elveum.store.builders.SimpleExternalQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleExternalQuerySuspendingBuilder
import com.elveum.store.builders.SimpleKeyedExternalQueryBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedExternalQueryBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.internal.stores.common.CoreFetcher
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow

internal class SimpleExternalQueryBuilderImpl<Q : Any, T : Any>(
    private val initialQueryProvider: (Unit) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: () -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleExternalQueryBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleExternalQueryBuilder<Q, T>, BaseBuilder<SimpleExternalQueryBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    private val externalQueryProvider: (Unit) -> Flow<Q> = { queryFlow() }

    override fun addSuspendingLocalStorage(): SimpleExternalQuerySuspendingBuilder<Q, T> {
        return SimpleExternalQuerySuspendingBuilderImpl(initialQueryProvider, queryDebounceMillis, queryFlow, config)
    }

    override fun addReactiveLocalStorage(): SimpleExternalQueryReactiveBuilder<Q, T> {
        return SimpleExternalQueryReactiveBuilderImpl(initialQueryProvider, queryDebounceMillis, queryFlow, config)
    }

    override fun disableFetcher(): SimpleExternalQueryReactiveNoFetcherBuilder<Q, T> {
        return SimpleExternalQueryReactiveNoFetcherBuilderImpl(
            initialQueryProvider, queryDebounceMillis, queryFlow, config,
        )
    }

    override fun <Key : Any> withKeys(): SimpleKeyedExternalQueryBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryBuilderImpl(
            initialQueryProvider = { initialQueryProvider(Unit) },
            queryDebounceMillis = queryDebounceMillis,
            queryFlow = { queryFlow() },
            config = config,
        )
    }

    override fun build(contract: SimpleQueryContract<Q, T>): SimpleStore<T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Q) -> T): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query -> onFetch(query) },
            externalQueryProvider = externalQueryProvider,
        ).asSimpleStore()
    }

    override fun buildCustom(loader: suspend Emitter<T>.(Q) -> Unit): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = CoreFetcher.Custom { _, query -> loader(query) },
            externalQueryProvider = externalQueryProvider,
        ).asSimpleStore()
    }
}
