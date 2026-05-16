package com.elveum.container.subject.lazy

import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ScopedLazyFlowSubject
import kotlinx.coroutines.CoroutineScope

internal class ScopedLazyFlowSubjectImpl<T>(
    val coroutineScope: CoroutineScope,
    val subject: LazyFlowSubject<T>,
) : ScopedLazyFlowSubject<T>,
    CoroutineScope by coroutineScope,
    LazyFlowSubject<T> by subject
