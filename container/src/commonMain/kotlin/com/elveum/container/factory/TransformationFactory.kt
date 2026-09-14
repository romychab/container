package com.elveum.container.factory

import com.elveum.container.subject.transformation.ContainerTransformation
import com.elveum.container.subject.transformation.EmptyContainerTransformation

/**
 * Factory for creating new container flow transformations. It is
 * used by [DefaultSubjectFactory] upon calling
 * [SubjectFactory.createSubject] and [SubjectFactory.createCache] if you
 * don't specify your custom transformation function in args.
 */
public interface TransformationFactory {

    public fun <T> create(): ContainerTransformation<T>

    public companion object : TransformationFactory {

        /**
         * Default implementation does not affect container flows.
         */
        override fun <T> create(): ContainerTransformation<T> {
            return EmptyContainerTransformation()
        }

    }

}
