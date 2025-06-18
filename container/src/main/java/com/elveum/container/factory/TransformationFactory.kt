package com.elveum.container.factory

import com.elveum.container.subject.transformation.ContainerTransformation

/**
 * Factory for creating new container flow transformations. It is
 * used by [DefaultContainerFactory] upon calling
 * [ContainerFactory.createSubject] and [ContainerFactory.createCache] if you
 * don't specify your custom transformation function in args.
 */
public interface TransformationFactory {

    public fun <T> create(): ContainerTransformation<T>

    public companion object : TransformationFactory {

        /**
         * Default implementation does not affect container flows.
         */
        override fun <T> create(): ContainerTransformation<T> {
            return { it }
        }

    }

}
