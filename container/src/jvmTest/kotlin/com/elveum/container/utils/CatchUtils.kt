package com.elveum.container.utils


inline fun <reified T : Exception> catch(block: () -> Unit): T {
    try {
        block()
    } catch (e: Exception) {
        if (e is T) {
            return e
        }
    }
    throw AssertionError("No expected exception: ${T::class.java.name}")
}
