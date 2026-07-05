package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveBuilder
import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQueryReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow

internal class SimpleKeyedExternalQueryReactiveBuilderImpl<Key : Any, Q : Any, T : Any>(
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: (Key) -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T>> = BaseBuilderImpl(config),
) : SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun disableFetcher(): SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryReactiveNoFetcherBuilderImpl(
            initialQueryProvider, queryDebounceMillis, queryFlow, config,
        )
    }

    override fun build(contract: SimpleKeyedQueryReactiveContract<Key, Q, T>): KeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onObserveStorage = contract::observeLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onObserveStorage: (Key, Q) -> Flow<T?>,
    ): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            fetcher = onFetch,
            saver = onSaveToStorage,
            observer = onObserveStorage,
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            externalQueryProvider = queryFlow,
        )
    }
}
