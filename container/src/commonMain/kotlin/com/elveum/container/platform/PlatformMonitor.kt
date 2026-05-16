package com.elveum.container.platform

internal expect class PlatformMonitor constructor() {
    inline fun <T> synchronized(block: () -> T): T
}
