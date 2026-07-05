package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleKeyedQueryReactiveBuilder
import com.elveum.store.builders.SimpleQueryReactiveBuilder
import com.elveum.store.builders.SimpleQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedQueryReactiveBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleQueryStore
import com.elveum.store.stores.simple.SimpleQueryStore
import kotlinx.coroutines.flow.Flow

internal class SimpleQueryReactiveBuilderImpl<Q : Any, T : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleQueryReactiveBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleQueryReactiveBuilder<Q, T>, BaseBuilder<SimpleQueryReactiveBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): SimpleKeyedQueryReactiveBuilder<Key, Q, T> {
        return SimpleKeyedQueryReactiveBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun disableFetcher(): SimpleQueryReactiveNoFetcherBuilder<Q, T> {
        return SimpleQueryReactiveNoFetcherBuilderImpl(
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
        )
    }

    override fun build(contract: SimpleQueryReactiveContract<Q, T>): SimpleQueryStore<Q, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onObserveStorage = contract::observeLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onObserveStorage: (Q) -> Flow<T?>
    ): SimpleQueryStore<Q, T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query -> onFetch(query) },
            saver = { _, query, value -> onSaveToStorage(query, value) },
            observer = { _, query -> onObserveStorage(query) },
        ).asSimpleQueryStore()
    }

}
