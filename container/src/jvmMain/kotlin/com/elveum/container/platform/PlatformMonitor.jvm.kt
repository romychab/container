package com.elveum.container.platform

internal actual class PlatformMonitor {

    actual inline fun <T> synchronized(
        block: () -> T,
    ): T {
        return synchronized(this, block)
    }

}
