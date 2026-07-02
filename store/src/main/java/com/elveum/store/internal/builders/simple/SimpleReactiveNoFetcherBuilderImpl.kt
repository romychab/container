package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleKeyedReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedReactiveNoFetcherBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class SimpleReactiveNoFetcherBuilderImpl<T : Any>(
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleReactiveNoFetcherBuilder<T>> = BaseBuilderImpl(config),
) : SimpleReactiveNoFetcherBuilder<T>, BaseBuilder<SimpleReactiveNoFetcherBuilder<T>> by sharedBuilder{

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): SimpleKeyedReactiveNoFetcherBuilder<Key, T> {
        return SimpleKeyedReactiveNoFetcherBuilderImpl(config)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long
    ): SimpleQueryReactiveNoFetcherBuilder<Q, T> {
        return SimpleQueryReactiveNoFetcherBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun build(contract: SimpleReactiveNoFetcherContract<T>): SimpleStore<T> {
        return build(onObserve = contract::observe)
    }

    override fun build(onObserve: () -> Flow<T>): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Unit, T>(
            initialQuery = Unit,
            fetcher = { _, _ -> onObserve().first() },
            saver = { _, _, _ -> },
            observer = { _, _ -> onObserve() },
            config = config,
        ).asSimpleStore()
    }

}
