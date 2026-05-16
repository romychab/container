package com.elveum.container.cache

import kotlinx.coroutines.CoroutineScope

public interface ScopedLazyCache<Arg, T> : LazyCache<Arg, T>, CoroutineScope
