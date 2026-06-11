package com.elveum.store.internal.stores.common

import kotlinx.coroutines.sync.Mutex

internal interface MutexOwner {
    val mutex: Mutex
}
