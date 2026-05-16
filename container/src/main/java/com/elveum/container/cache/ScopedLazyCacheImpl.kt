package com.elveum.container.cache

import kotlinx.coroutines.CoroutineScope

internal class ScopedLazyCacheImpl<Arg, T>(
    private val lazyCache: LazyCache<Arg, T>,
    private val coroutineScope: CoroutineScope,
) : ScopedLazyCache<Arg, T>,
    LazyCache<Arg, T> by lazyCache,
    CoroutineScope by coroutineScope
