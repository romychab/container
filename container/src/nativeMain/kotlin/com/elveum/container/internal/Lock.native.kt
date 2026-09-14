package com.elveum.container.internal

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal actual class Lock actual constructor() {
    @PublishedApi
    internal val delegate: SynchronizedObject = SynchronizedObject()
}

internal actual inline fun <T> Lock.withLock(block: () -> T): T {
    return synchronized(delegate, block)
}
