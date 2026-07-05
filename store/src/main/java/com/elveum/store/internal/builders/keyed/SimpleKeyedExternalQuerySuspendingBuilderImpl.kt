package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.SimpleKeyedExternalQuerySuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQuerySuspendingContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow

internal class SimpleKeyedExternalQuerySuspendingBuilderImpl<Key : Any, Q : Any, T : Any>(
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: (Key) -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T>> = BaseBuilderImpl(config),
) : SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T>,
    BaseBuilder<SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun build(contract: SimpleKeyedQuerySuspendingContract<Key, Q, T>): KeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onLoadFromStorage: suspend (Key, Q) -> T?,
    ): KeyedStore<Key, T> {
        return KeyedQueryStoreImpl(
            fetcher = onFetch,
            loader = onLoadFromStorage,
            saver = onSaveToStorage,
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            externalQueryProvider = queryFlow,
        )
    }
}
