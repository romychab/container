package com.elveum.container.subject

import kotlinx.coroutines.CoroutineScope

public interface ScopedLazyFlowSubject<T> : LazyFlowSubject<T>, CoroutineScope
