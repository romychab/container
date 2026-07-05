package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQueryReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class SimpleKeyedExternalQueryReactiveNoFetcherBuilderImpl<Key : Any, Q : Any, T : Any>(
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: (Key) -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T>> =
        BaseBuilderImpl(config),
) : SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun build(contract: SimpleKeyedQueryReactiveNoFetcherContract<Key, Q, T>): KeyedStore<Key, T> {
        return build(onObserve = contract::observe)
    }

    override fun build(onObserve: (Key, Q) -> Flow<T>): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            fetcher = { key, query -> onObserve(key, query).first() },
            saver = { _, _, _ -> },
            observer = { key, query -> onObserve(key, query) },
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            externalQueryProvider = queryFlow,
        )
    }
}
