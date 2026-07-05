package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedExternalQueryReactiveBuilder
import com.elveum.store.builders.SimpleKeyedQueryReactiveBuilder
import com.elveum.store.builders.SimpleKeyedReactiveBuilder
import com.elveum.store.builders.SimpleKeyedReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class SimpleKeyedReactiveBuilderImpl<Key : Any, T : Any>(
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedReactiveBuilder<Key, T>> = BaseBuilderImpl(config)
) : SimpleKeyedReactiveBuilder<Key, T>, BaseBuilder<SimpleKeyedReactiveBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun disableFetcher(): SimpleKeyedReactiveNoFetcherBuilder<Key, T> {
        return SimpleKeyedReactiveNoFetcherBuilderImpl(config)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long
    ): SimpleKeyedQueryReactiveBuilder<Key, Q, T> {
        return SimpleKeyedQueryReactiveBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long,
        queryFlow: (Key) -> Flow<Q>,
    ): SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryReactiveBuilderImpl(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun <Q : Any> withQuery(
        debounceMillis: Long,
        queryFlow: (Key) -> StateFlow<Q>,
    ): SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T> {
        return SimpleKeyedExternalQueryReactiveBuilderImpl(
            initialQueryProvider = { key -> queryFlow(key).value },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun build(contract: SimpleKeyedReactiveContract<Key, T>): KeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onObserveStorage = contract::observeLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onObserveStorage: (Key) -> Flow<T?>
    ): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            config = config,
            fetcher = { key, _ -> onFetch(key) },
            saver = { key, _, value -> onSaveToStorage(key, value) },
            observer = { key, _ -> onObserveStorage(key) },
            initialQueryProvider = { Unit },
        )
    }
}
