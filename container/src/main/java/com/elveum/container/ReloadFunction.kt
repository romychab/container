package com.elveum.container

import com.elveum.container.cache.LazyCache
import com.elveum.container.subject.LazyFlowSubject

/**
 * Function for reloading data encapsulated within [Container] instances
 * emitted by [LazyFlowSubject] or [LazyCache].
 */
public typealias ReloadFunction = (silently: Boolean) -> Unit

/**
 * Default empty reload function that does not load anything.
 */
public val EmptyReloadFunction: ReloadFunction = object : ReloadFunction {
    override fun invoke(silently: Boolean) = Unit

    override fun toString(): String {
        return "EmptyReloadFunction"
    }
}
