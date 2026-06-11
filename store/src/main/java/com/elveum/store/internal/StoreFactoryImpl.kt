package com.elveum.store.internal

import com.elveum.store.builders.PagedBuilder
import com.elveum.store.StoreFactory
import com.elveum.store.builders.KeyedBuilder
import com.elveum.store.builders.SimpleBuilder
import com.elveum.store.internal.builders.keyed.KeyedBuilderImpl
import com.elveum.store.internal.builders.paged.PagedBuilderImpl
import com.elveum.store.internal.builders.simple.SimpleBuilderImpl

internal object StoreFactoryImpl : StoreFactory {

    override fun <T : Any> simpleStoreBuilder(): SimpleBuilder<T> = SimpleBuilderImpl()

    override fun <PageKey : Any, T : Any> pagedStoreBuilder(
        initialKey: PageKey,
        itemId: (T) -> Any
    ): PagedBuilder<PageKey, T> {
        return PagedBuilderImpl(initialKey, itemId)
    }

    override fun <Key : Any, T : Any> keyedStoreBuilder(): KeyedBuilder<Key, T> {
        return KeyedBuilderImpl()
    }

}
