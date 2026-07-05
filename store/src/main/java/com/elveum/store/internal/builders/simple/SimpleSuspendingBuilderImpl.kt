package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleExternalQuerySuspendingBuilder
import com.elveum.store.builders.SimpleKeyedSuspendingBuilder
import com.elveum.store.builders.SimpleQuerySuspendingBuilder
import com.elveum.store.builders.SimpleSuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleSuspendingContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedSuspendingBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class SimpleSuspendingBuilderImpl<T : Any>(
    private val config: SharedConfig,
    private val sharedBuilder: BaseBuilderImpl<SimpleSuspendingBuilder<T>> = BaseBuilderImpl(config),
) : SimpleSuspendingBuilder<T>, BaseBuilder<SimpleSuspendingBuilder<T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): SimpleKeyedSuspendingBuilder<Key, T> {
        return SimpleKeyedSuspendingBuilderImpl(config)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): SimpleQuerySuspendingBuilder<Q, T> {
        return SimpleQuerySuspendingBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long,
        queryFlow: () -> Flow<Q>,
    ): SimpleExternalQuerySuspendingBuilder<Q, T> {
        return SimpleExternalQuerySuspendingBuilderImpl(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun <Q : Any> withQuery(
        debounceMillis: Long,
        queryFlow: () -> StateFlow<Q>,
    ): SimpleExternalQuerySuspendingBuilder<Q, T> {
        return SimpleExternalQuerySuspendingBuilderImpl(
            initialQueryProvider = { queryFlow().value },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun build(contract: SimpleSuspendingContract<T>): SimpleStore<T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend () -> T,
        onSaveToStorage: suspend (T) -> Unit,
        onLoadFromStorage: suspend () -> T?
    ): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Unit, T>(
            initialQueryProvider = { Unit },
            config = config,
            fetcher = { _, _ -> onFetch() },
            saver = { _, _, data -> onSaveToStorage(data) },
            loader = { _, _ -> onLoadFromStorage() },
        ).asSimpleStore()
    }

}
