package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.SimpleQueryStoreImpl
import com.elveum.store.internal.stores.SimpleStoreImpl
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
        return SimpleStoreImpl(
            SimpleQueryStoreImpl(
                initialQuery = Unit,
                fetcher = { onObserve().first() },
                saver = { _, _ -> },
                observer = { onObserve() },
                config = config,
            )
        )
    }

}
