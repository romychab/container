package com.elveum.container.subject.transformation

import com.elveum.container.Container
import kotlinx.coroutines.flow.Flow

internal class EmptyContainerTransformation<T> : ContainerTransformation<T> {

    override fun invoke(flow: Flow<Container<T>>): Flow<Container<T>> {
        return flow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmptyContainerTransformation<*>) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

}
