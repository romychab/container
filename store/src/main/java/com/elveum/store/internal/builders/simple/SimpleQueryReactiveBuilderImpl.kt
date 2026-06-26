package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleQueryReactiveBuilder
import com.elveum.store.builders.SimpleQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.SimpleQueryStoreImpl
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
        return SimpleQueryStoreImpl(
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = onFetch,
            saver = onSaveToStorage,
            observer = onObserveStorage,
        )
    }

}
