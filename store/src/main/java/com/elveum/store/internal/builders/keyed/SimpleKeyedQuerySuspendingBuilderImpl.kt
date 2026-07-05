package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedQuerySuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQuerySuspendingContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedQueryStore

internal class SimpleKeyedQuerySuspendingBuilderImpl<Key : Any, Q : Any, T : Any>(
    val initialQuery: Q,
    val queryDebounceMillis: Long,
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedQuerySuspendingBuilder<Key, Q, T>> = BaseBuilderImpl(config),
) : SimpleKeyedQuerySuspendingBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedQuerySuspendingBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onLoadFromStorage: suspend (Key, Q) -> T?
    ): KeyedQueryStore<Key, Q, T> {
        return KeyedQueryStoreImpl(
            fetcher = onFetch,
            loader = onLoadFromStorage,
            saver = onSaveToStorage,
            config = config,
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
        )
    }

    override fun build(contract: SimpleKeyedQuerySuspendingContract<Key, Q, T>): KeyedQueryStore<Key, Q, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }
}
