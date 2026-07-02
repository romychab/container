package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleKeyedReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class SimpleKeyedReactiveNoFetcherBuilderImpl<Key : Any, T : Any>(
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedReactiveNoFetcherBuilder<Key, T>> = BaseBuilderImpl(config)
) : SimpleKeyedReactiveNoFetcherBuilder<Key, T>,
    BaseBuilder<SimpleKeyedReactiveNoFetcherBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long
    ): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T> {
        return SimpleKeyedQueryReactiveNoFetcherBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun build(contract: SimpleKeyedReactiveNoFetcherContract<Key, T>): KeyedStore<Key, T> {
        return build(onObserve = contract::observe)
    }

    override fun build(onObserve: (Key) -> Flow<T>): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            fetcher = { key, _ -> onObserve(key).first() },
            saver = { _, _, _ -> },
            observer = { key, _ -> onObserve(key) },
            initialQuery = Unit,
            config = config,
        )
    }
}
