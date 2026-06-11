package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleQuerySuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQuerySuspendingContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.SimpleQueryStoreImpl
import com.elveum.store.stores.simple.SimpleQueryStore

internal class SimpleQuerySuspendingBuilderImpl<Q : Any, T : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val config: SharedConfig,
    private val sharedBuilder: BaseBuilderImpl<SimpleQuerySuspendingBuilder<Q, T>> = BaseBuilderImpl(config),
) : SimpleQuerySuspendingBuilder<Q, T>, BaseBuilder<SimpleQuerySuspendingBuilder<Q, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun build(contract: SimpleQuerySuspendingContract<Q, T>): SimpleQueryStore<Q, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onLoadFromStorage: suspend (Q) -> T?
    ): SimpleQueryStore<Q, T> {
        return SimpleQueryStoreImpl(
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = onFetch,
            saver = onSaveToStorage,
            loader = onLoadFromStorage,
        )
    }

}
