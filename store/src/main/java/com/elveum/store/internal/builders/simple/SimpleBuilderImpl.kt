package com.elveum.store.internal.builders.simple

import com.elveum.store.builders.SimpleBuilder
import com.elveum.store.builders.SimpleQueryBuilder
import com.elveum.store.builders.SimpleReactiveBuilder
import com.elveum.store.builders.SimpleSuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.stores.SimpleQueryStoreImpl
import com.elveum.store.internal.stores.asSimpleStore
import com.elveum.store.stores.simple.SimpleStore

internal class SimpleBuilderImpl<T : Any>(
    val sharedBuilder: BaseBuilderImpl<SimpleBuilder<T>> = BaseBuilderImpl(),
) : SimpleBuilder<T>, BaseBuilder<SimpleBuilder<T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): SimpleQueryBuilder<Q, T> {
        return SimpleQueryBuilderImpl(initialQuery, debounceMillis, sharedBuilder.config)
    }

    override fun addSuspendingLocalStorage(): SimpleSuspendingBuilder<T> {
        return SimpleSuspendingBuilderImpl(sharedBuilder.config)
    }

    override fun addReactiveLocalStorage(): SimpleReactiveBuilder<T> {
        return SimpleReactiveBuilderImpl(sharedBuilder.config)
    }

    override fun build(onFetch: suspend () -> T): SimpleStore<T> {
        return SimpleQueryStoreImpl(
            initialQuery = Unit,
            config = sharedBuilder.config,
            fetcher = { onFetch() },
        ).asSimpleStore()
    }

    override fun build(contract: SimpleContract<T>): SimpleStore<T> {
        return build(onFetch = contract::fetch)
    }

}
