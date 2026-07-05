package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleExternalQuerySuspendingBuilder
import com.elveum.store.builders.SimpleKeyedExternalQuerySuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQuerySuspendingContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.builders.keyed.SimpleKeyedExternalQuerySuspendingBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow

internal class SimpleExternalQuerySuspendingBuilderImpl<Q : Any, T : Any>(
    private val initialQueryProvider: (Unit) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: () -> Flow<Q>,
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleExternalQuerySuspendingBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleExternalQuerySuspendingBuilder<Q, T>,
    BaseBuilder<SimpleExternalQuerySuspendingBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    private val externalQueryProvider: (Unit) -> Flow<Q> = { queryFlow() }

    override fun <Key : Any> withKeys(): SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T> {
        return SimpleKeyedExternalQuerySuspendingBuilderImpl(
            initialQueryProvider = { initialQueryProvider(Unit) },
            queryDebounceMillis = queryDebounceMillis,
            queryFlow = { queryFlow() },
            config = config,
        )
    }

    override fun build(contract: SimpleQuerySuspendingContract<Q, T>): SimpleStore<T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onLoadFromStorage: suspend (Q) -> T?,
    ): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Q, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query -> onFetch(query) },
            saver = { _, query, value -> onSaveToStorage(query, value) },
            loader = { _, query -> onLoadFromStorage(query) },
            externalQueryProvider = externalQueryProvider,
        ).asSimpleStore()
    }
}
