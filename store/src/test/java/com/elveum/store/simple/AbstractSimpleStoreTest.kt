package com.elveum.store.simple

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.builders.SimpleBuilder

abstract class AbstractSimpleStoreTest : AbstractStoreTest() {

    protected fun storeBuilder(): SimpleBuilder<String> = StoreFactory
        .simpleStoreBuilder<String>()
        .setCoroutineScopeFactory(createStoreScopeFactory())

}
