package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleQueryReactiveBuilder
import com.elveum.store.builders.SimpleReactiveBuilder
import com.elveum.store.builders.SimpleReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.SimpleQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow

internal class SimpleReactiveBuilderImpl<T : Any>(
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<SimpleReactiveBuilder<T>> = BaseBuilderImpl(config),
) : SimpleReactiveBuilder<T>, BaseBuilder<SimpleReactiveBuilder<T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): SimpleQueryReactiveBuilder<Q, T> {
        return SimpleQueryReactiveBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun disableFetcher(): SimpleReactiveNoFetcherBuilder<T> {
        return SimpleReactiveNoFetcherBuilderImpl(config)
    }

    override fun build(
        onFetch: suspend () -> T,
        onSaveToStorage: suspend (T) -> Unit,
        onObserveStorage: () -> Flow<T?>
    ): SimpleStore<T> {
        return SimpleQueryStoreImpl(
            initialQuery = Unit,
            config = config,
            fetcher = { onFetch() },
            saver = { _, data -> onSaveToStorage(data) },
            observer = { onObserveStorage() }
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
