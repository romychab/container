package com.elveum.container.cache

import com.elveum.container.subject.ValueLoader

/**
 * Create a new [ValueLoader] instance for the specific input argument.
 */
public fun interface ValueLoaderFactory<Arg, T> {
    public fun create(arg: Arg): ValueLoader<T>
}
