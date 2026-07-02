package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQueryReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedQueryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class SimpleKeyedQueryReactiveNoFetcherBuilderImpl<Key : Any, Q : Any, T : Any>(
    val initialQuery: Q,
    val queryDebounceMillis: Long,
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>> = BaseBuilderImpl(config),
) : SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun build(onObserve: (Key, Q) -> Flow<T>): KeyedQueryStore<Key, Q, T> {
        return KeyedQueryStoreImpl(
            fetcher = { key, query -> onObserve(key, query).first() },
            saver = { _, _, _ -> },
            observer = { key, query -> onObserve(key, query) },
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
        )
    }

    override fun build(contract: SimpleKeyedQueryReactiveNoFetcherContract<Key, Q, T>): KeyedQueryStore<Key, Q, T> {
        return build(onObserve = contract::observe)
    }
}
