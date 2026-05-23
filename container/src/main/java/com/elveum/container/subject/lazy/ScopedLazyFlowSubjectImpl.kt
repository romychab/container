package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.subject.ContainerConfiguration
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ScopedLazyFlowSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class ScopedLazyFlowSubjectImpl<T>(
    val spyMode: Boolean,
    val coroutineScope: CoroutineScope,
    val subject: LazyFlowSubject<T>,
) : ScopedLazyFlowSubject<T>,
    CoroutineScope by coroutineScope,
    LazyFlowSubject<T> by subject {

    override fun listen(configuration: ContainerConfiguration): StateFlow<Container<T>> {
        return if (spyMode) {
            subject.spy(configuration)
        } else {
            subject.listen(configuration)
        }
    }

}
