package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedQueryReactiveBuilder
import com.elveum.store.builders.SimpleKeyedQueryReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQueryReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedQueryStore
import kotlinx.coroutines.flow.Flow

internal class SimpleKeyedQueryReactiveBuilderImpl<Key : Any, Q : Any, T : Any>(
    val initialQuery: Q,
    val queryDebounceMillis: Long,
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedQueryReactiveBuilder<Key, Q, T>> = BaseBuilderImpl(config),
) : SimpleKeyedQueryReactiveBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedQueryReactiveBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun disableFetcher(): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T> {
        return SimpleKeyedQueryReactiveNoFetcherBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onObserveStorage: (Key, Q) -> Flow<T?>
    ): KeyedQueryStore<Key, Q, T> {
        return KeyedQueryStoreImpl(
            fetcher = onFetch,
            saver = onSaveToStorage,
            observer = onObserveStorage,
            config = config,
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
        )
    }

    override fun build(contract: SimpleKeyedQueryReactiveContract<Key, Q, T>): KeyedQueryStore<Key, Q, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onObserveStorage = contract::observeLocalStorage,
        )
    }
}
