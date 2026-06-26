package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.KeyedBuilder
import com.elveum.store.builders.KeyedReactiveBuilder
import com.elveum.store.builders.KeyedReactiveNoFetcherBuilder
import com.elveum.store.builders.KeyedSuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.KeyedContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.stores.KeyedStoreImpl
import com.elveum.store.stores.keyed.KeyedStore

internal class KeyedBuilderImpl<Key : Any, T : Any>(
    val sharedBuilder: BaseBuilderImpl<KeyedBuilder<Key, T>> = BaseBuilderImpl(),
) : KeyedBuilder<Key, T>, BaseBuilder<KeyedBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun disableFetcher(): KeyedReactiveNoFetcherBuilder<Key, T> {
        return KeyedReactiveNoFetcherBuilderImpl(sharedBuilder.config)
    }

    override fun addSuspendingLocalStorage(): KeyedSuspendingBuilder<Key, T> {
        return KeyedSuspendingBuilderImpl(sharedBuilder.config)
    }

    override fun addReactiveLocalStorage(): KeyedReactiveBuilder<Key, T> {
        return KeyedReactiveBuilderImpl(sharedBuilder.config)
    }

    override fun build(contract: KeyedContract<Key, T>): KeyedStore<Key, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Key) -> T): KeyedStore<Key, T> {
        return KeyedStoreImpl(
            config = sharedBuilder.config,
            fetcher = onFetch,
        )
    }

}
