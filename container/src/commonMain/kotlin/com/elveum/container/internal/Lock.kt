package com.elveum.container.internal

/**
 * A reentrant mutual-exclusion lock.
 *
 * On JVM this is backed by the built-in monitor, so [withLock] compiles to the
 * same `synchronized` intrinsic the library used before. On Kotlin/Native it is
 * backed by a reentrant lock with identical semantics.
 */
internal expect class Lock() {
    // no members: the lock is only ever used through [withLock]
}

/**
 * Run [block] while holding [this] lock.
 *
 * Reentrant: a thread that already holds the lock may re-acquire it without
 * deadlocking.
 */
internal expect inline fun <T> Lock.withLock(block: () -> T): T
