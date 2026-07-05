package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleExternalQueryReactiveBuilder
import com.elveum.store.builders.SimpleKeyedReactiveBuilder
import com.elveum.store.builders.SimpleQueryReactiveBuilder
import com.elveum.store.builders.SimpleReactiveBuilder
import com.elveum.store.builders.SimpleReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedReactiveBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class SimpleReactiveBuilderImpl<T : Any>(
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleReactiveBuilder<T>> = BaseBuilderImpl(config),
) : SimpleReactiveBuilder<T>, BaseBuilder<SimpleReactiveBuilder<T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): SimpleKeyedReactiveBuilder<Key, T> {
        return SimpleKeyedReactiveBuilderImpl(config)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): SimpleQueryReactiveBuilder<Q, T> {
        return SimpleQueryReactiveBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long,
        queryFlow: () -> Flow<Q>,
    ): SimpleExternalQueryReactiveBuilder<Q, T> {
        return SimpleExternalQueryReactiveBuilderImpl(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun <Q : Any> withQuery(
        debounceMillis: Long,
        queryFlow: () -> StateFlow<Q>,
    ): SimpleExternalQueryReactiveBuilder<Q, T> {
        return SimpleExternalQueryReactiveBuilderImpl(
            initialQueryProvider = { queryFlow().value },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun disableFetcher(): SimpleReactiveNoFetcherBuilder<T> {
        return SimpleReactiveNoFetcherBuilderImpl(config)
    }

    override fun build(
        onFetch: suspend () -> T,
        onSaveToStorage: suspend (T) -> Unit,
        onObserveStorage: () -> Flow<T?>
    ): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Unit, T>(
            initialQueryProvider = { Unit },
            config = config,
            fetcher = { _, _ -> onFetch() },
            saver = { _, _, data -> onSaveToStorage(data) },
            observer = { _, _ -> onObserveStorage() }
        ).asSimpleStore()
    }

    override fun build(contract: SimpleReactiveContract<T>): SimpleStore<T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onObserveStorage = contract::observeLocalStorage,
        )
    }

}
