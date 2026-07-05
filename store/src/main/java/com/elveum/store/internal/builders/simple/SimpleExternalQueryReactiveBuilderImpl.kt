package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleExternalQueryReactiveBuilder
import com.elveum.store.builders.SimpleExternalQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedExternalQueryReactiveBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow

internal class SimpleExternalQueryReactiveBuilderImpl<Q : Any, T : Any>(
    private val initialQueryProvider: (Unit) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: () -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleExternalQueryReactiveBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleExternalQueryReactiveBuilder<Q, T>,
    BaseBuilder<SimpleExternalQueryReactiveBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    private val externalQueryProvider: (Unit) -> Flow<Q> = { queryFlow() }

    override fun disableFetcher(): SimpleExternalQueryReactiveNoFetcherBuilder<Q, T> {
        return SimpleExternalQueryReactiveNoFetcherBuilderImpl(
            initialQueryProvider, queryDebounceMillis, queryFlow, config,
        )
    }

    override fun <Key : Any> withKeys(): SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryReactiveBuilderImpl(
            initialQueryProvider = { initialQueryProvider(Unit) },
            queryDebounceMillis = queryDebounceMillis,
            queryFlow = { queryFlow() },
            config = config,
        )
    }

    override fun build(contract: SimpleQueryReactiveContract<Q, T>): SimpleStore<T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onObserveStorage = contract::observeLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onObserveStorage: (Q) -> Flow<T?>,
    ): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query -> onFetch(query) },
            saver = { _, query, value -> onSaveToStorage(query, value) },
            observer = { _, query -> onObserveStorage(query) },
            externalQueryProvider = externalQueryProvider,
        ).asSimpleStore()
    }
}
