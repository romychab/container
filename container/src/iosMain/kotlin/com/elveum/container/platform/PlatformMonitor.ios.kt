package com.elveum.container.platform

import platform.Foundation.NSLock

internal actual class PlatformMonitor {

    val lock: NSLock = NSLock()

    actual inline fun <T> synchronized(
        block: () -> T,
    ): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

}
