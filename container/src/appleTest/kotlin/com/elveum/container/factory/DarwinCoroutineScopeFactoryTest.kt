package com.elveum.container.factory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Darwin-only checks for the default [CoroutineScopeFactory], which builds every
 * subject's scope on `Dispatchers.Main.immediate`.
 *
 * These run on Apple targets only. `linuxX64` has no Main dispatcher at all, so
 * this file deliberately lives in `appleTest` rather than `nativeTest`.
 *
 * What this proves: that `Dispatchers.Main` exists on Darwin without an extra
 * artifact, that `.immediate` is implemented rather than throwing
 * `UnsupportedOperationException`, and that the default factory can build a live
 * scope. What it does NOT prove: end-to-end delivery of work onto the main queue,
 * which needs a running run loop and is exercised by a real app rather than a
 * unit test.
 */
class DarwinCoroutineScopeFactoryTest {

    @Test
    fun dispatchersMain_isAvailableWithoutAnExtraArtifact() {
        assertNotNull(Dispatchers.Main)
    }

    @Test
    fun dispatchersMainImmediate_isImplementedOnDarwin() {
        // MainCoroutineDispatcher.immediate throws UnsupportedOperationException on
        // platforms that do not override it. Darwin does; this pins that.
        val immediate = Dispatchers.Main.immediate
        assertNotNull(immediate)
    }

    @Test
    fun defaultFactory_createsALiveScopeBoundToTheMainDispatcher() {
        val scope: CoroutineScope = CoroutineScopeFactory.createScope()
        try {
            assertTrue(scope.isActive)
            // Identity is not asserted: `immediate` is not contractually a singleton.
            val interceptor = scope.coroutineContext[ContinuationInterceptor]
            assertNotNull(interceptor)
            assertTrue(interceptor is MainCoroutineDispatcher)
        } finally {
            scope.cancel()
        }
    }
}
