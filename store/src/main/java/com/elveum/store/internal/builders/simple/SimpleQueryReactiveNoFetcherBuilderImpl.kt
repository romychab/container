package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.SimpleQueryStoreImpl
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

    override fun build(contract: SimpleQueryReactiveNoFetcherContract<Q, T>): SimpleQueryStore<Q, T> {
        return build(
            onObserve = { contract.observe(it) }
        )
    }

    override fun build(onObserve: (Q) -> Flow<T>): SimpleQueryStore<Q, T> {
        return SimpleQueryStoreImpl(
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { onObserve(it).first() },
            saver = { _, _ -> },
            observer = { onObserve(it) }
        )
    }

}
