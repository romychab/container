package com.elveum.store.internal.builders.simple

import com.elveum.container.Emitter
import com.elveum.store.builders.SimpleBuilder
import com.elveum.store.builders.SimpleExternalQueryBuilder
import com.elveum.store.builders.SimpleKeyedBuilder
import com.elveum.store.builders.SimpleQueryBuilder
import com.elveum.store.builders.SimpleReactiveBuilder
import com.elveum.store.builders.SimpleReactiveNoFetcherBuilder
import com.elveum.store.builders.SimpleSuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.keyed.SimpleKeyedBuilderImpl
import com.elveum.store.internal.stores.KeyedQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.internal.stores.common.CoreFetcher
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class SimpleBuilderImpl<T : Any>(
    val sharedBuilder: BaseBuilderImpl<SimpleBuilder<T>> = BaseBuilderImpl(),
) : SimpleBuilder<T>, BaseBuilder<SimpleBuilder<T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): SimpleQueryBuilder<Q, T> {
        return SimpleQueryBuilderImpl(initialQuery, debounceMillis, sharedBuilder.config)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long,
        queryFlow: () -> Flow<Q>,
    ): SimpleExternalQueryBuilder<Q, T> {
        return SimpleExternalQueryBuilderImpl(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = sharedBuilder.config,
        )
    }

    override fun <Q : Any> withQuery(
        debounceMillis: Long,
        queryFlow: () -> StateFlow<Q>,
    ): SimpleExternalQueryBuilder<Q, T> {
        return SimpleExternalQueryBuilderImpl(
            initialQueryProvider = { queryFlow().value },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = sharedBuilder.config,
        )
    }

    override fun <Key : Any> withKeys(): SimpleKeyedBuilder<Key, T> {
        return SimpleKeyedBuilderImpl(sharedBuilder.config)
    }

    override fun addSuspendingLocalStorage(): SimpleSuspendingBuilder<T> {
        return SimpleSuspendingBuilderImpl(sharedBuilder.config)
    }

    override fun addReactiveLocalStorage(): SimpleReactiveBuilder<T> {
        return SimpleReactiveBuilderImpl(sharedBuilder.config)
    }

    override fun disableFetcher(): SimpleReactiveNoFetcherBuilder<T> {
        return SimpleReactiveNoFetcherBuilderImpl(sharedBuilder.config)
    }

    override fun build(onFetch: suspend () -> T): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Unit, T>(
            fetcher = { _, _ -> onFetch() },
            config = sharedBuilder.config,
            initialQueryProvider = { Unit },
        ).asSimpleStore()
    }

    override fun buildCustom(loader: suspend Emitter<T>.() -> Unit): SimpleStore<T> {
        return KeyedQueryStoreImpl<Unit, Unit, T>(
            fetcher = CoreFetcher.Custom { _, _ -> loader() },
            config = sharedBuilder.config,
            initialQueryProvider = { Unit },
        ).asSimpleStore()
    }

    override fun build(contract: SimpleContract<T>): SimpleStore<T> {
        return build(onFetch = contract::fetch)
    }

}
