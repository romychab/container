package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedQuerySuspendingBuilder
import com.elveum.store.builders.SimpleKeyedSuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedSuspendingContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedStore

internal class SimpleKeyedSuspendingBuilderImpl<Key : Any, T : Any>(
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedSuspendingBuilder<Key, T>> = BaseBuilderImpl(config),
) : SimpleKeyedSuspendingBuilder<Key, T>, BaseBuilder<SimpleKeyedSuspendingBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long
    ): SimpleKeyedQuerySuspendingBuilder<Key, Q, T> {
        return SimpleKeyedQuerySuspendingBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun build(contract: SimpleKeyedSuspendingContract<Key, T>): KeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onLoadFromStorage: suspend (Key) -> T?
    ): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            config = config,
            fetcher = { key, _ -> onFetch(key) },
            saver = { key, _, value -> onSaveToStorage(key, value) },
            loader = { key, _ -> onLoadFromStorage(key) },
            initialQuery = Unit,
        )
    }
}
