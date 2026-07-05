package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleKeyedQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedQueryReactiveNoFetcherBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleQueryStore
import com.elveum.store.stores.simple.SimpleQueryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class SimpleQueryReactiveNoFetcherBuilderImpl<Q : Any, T : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleQueryReactiveNoFetcherBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleQueryReactiveNoFetcherBuilder<Q, T>, BaseBuilder<SimpleQueryReactiveNoFetcherBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T> {
        return SimpleKeyedQueryReactiveNoFetcherBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun build(contract: SimpleQueryReactiveNoFetcherContract<Q, T>): SimpleQueryStore<Q, T> {
        return build(
            onObserve = { contract.observe(it) }
        )
    }

    override fun build(onObserve: (Q) -> Flow<T>): SimpleQueryStore<Q, T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query -> onObserve(query).first() },
            saver = { _, _, _ -> },
            observer = { _, query -> onObserve(query) }
        ).asSimpleQueryStore()
    }

}
