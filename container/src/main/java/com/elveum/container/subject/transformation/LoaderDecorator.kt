package com.elveum.container.subject.transformation

import com.elveum.container.FlowComposer

/**
 * Optional decorator that wraps any loader function in subject and/or
 * cache instances. It may be used to add common logic to all loaders, e.g.
 * by creating custom [com.elveum.container.factory.DefaultSubjectFactory]
 * factory instance.
 */
public fun interface LoaderDecorator {

    /**
     * Decorate the loader function.
     *
     * Note: you must call [originLoader] lambda parameter within the implementation body.
     */
    public suspend fun FlowComposer.decorate(
        originLoader: suspend () -> Unit
    )

    public companion object : LoaderDecorator {
        override suspend fun FlowComposer.decorate(originLoader: suspend () -> Unit) {
            originLoader()
        }
    }

}
