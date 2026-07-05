package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleExternalQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedExternalQueryReactiveNoFetcherBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class SimpleExternalQueryReactiveNoFetcherBuilderImpl<Q : Any, T : Any>(
    private val initialQueryProvider: (Unit) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: () -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleExternalQueryReactiveNoFetcherBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleExternalQueryReactiveNoFetcherBuilder<Q, T>,
    BaseBuilder<SimpleExternalQueryReactiveNoFetcherBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    private val externalQueryProvider: (Unit) -> Flow<Q> = { queryFlow() }

    override fun <Key : Any> withKeys(): SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryReactiveNoFetcherBuilderImpl(
            initialQueryProvider = { initialQueryProvider(Unit) },
            queryDebounceMillis = queryDebounceMillis,
            queryFlow = { queryFlow() },
            config = config,
        )
    }

    override fun build(contract: SimpleQueryReactiveNoFetcherContract<Q, T>): SimpleStore<T> {
        return build(onObserve = { contract.observe(it) })
    }

    override fun build(onObserve: (Q) -> Flow<T>): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query -> onObserve(query).first() },
            saver = { _, _, _ -> },
            observer = { _, query -> onObserve(query) },
            externalQueryProvider = externalQueryProvider,
        ).asSimpleStore()
    }
}
