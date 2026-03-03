package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.pendingContainer
import kotlinx.coroutines.Job
import kotlin.coroutines.Continuation

internal class PageKeyRecord<Key, T>(
    val key: Key,
    var job: Job? = null,
    var container: Container<List<T>> = pendingContainer(),
    var retryContinuation: Continuation<Unit>? = null,
)
