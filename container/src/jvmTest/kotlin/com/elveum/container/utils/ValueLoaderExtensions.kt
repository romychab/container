package com.elveum.container.utils

import com.elveum.container.Emitter
import com.elveum.container.subject.ValueLoader

suspend fun <T> ValueLoader<T>.invokeOn(emitter: Emitter<T>) {
    emitter.invoke()
}
