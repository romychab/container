package com.elveum.store.keyed

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.builders.SimpleKeyedBuilder

abstract class AbstractKeyedStoreTest : AbstractStoreTest() {

    protected fun storeBuilder(): SimpleKeyedBuilder<String, String> = StoreFactory
        .simpleStoreBuilder<String>()
        .withKeys<String>()
        .setCoroutineScopeFactory(createStoreScopeFactory())

}
