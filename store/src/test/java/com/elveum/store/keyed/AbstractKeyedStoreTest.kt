package com.elveum.store.keyed

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.builders.KeyedBuilder

abstract class AbstractKeyedStoreTest : AbstractStoreTest() {

    protected fun storeBuilder(): KeyedBuilder<String, String> = StoreFactory
        .keyedStoreBuilder<String, String>()
        .setCoroutineScopeFactory(createStoreScopeFactory())

}
