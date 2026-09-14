package com.elveum.container.internal

internal actual class Lock actual constructor()

internal actual inline fun <T> Lock.withLock(block: () -> T): T {
    return kotlin.synchronized(this, block)
}
