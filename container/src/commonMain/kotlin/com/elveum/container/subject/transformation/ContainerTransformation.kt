package com.elveum.container.subject.transformation

import com.elveum.container.Container
import com.elveum.container.cache.LazyCache
import com.elveum.container.subject.LazyFlowSubject
import kotlinx.coroutines.flow.Flow

/**
 * Transformation function for any container flow emitted by [LazyFlowSubject]
 * and [LazyCache].
 */
public typealias ContainerTransformation<T> = (Flow<Container<T>>) -> Flow<Container<T>>
