package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.Emitter
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class LazyCacheExtensionsTest {

    @MockK(relaxUnitFun = true)
    private lateinit var lazyCache: LazyCache<String, String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun reloadAsync_delegatesCallToNewLoad() {
        every { lazyCache.reload(any(), any()) } returns emptyFlow()

        lazyCache.reloadAsync("arg1", false)
        lazyCache.reloadAsync("arg2", true)

        verify(exactly = 1) {
            lazyCache.reload("arg1", false)
            lazyCache.reload("arg2", true)
        }
    }

    @Test
    fun updateWith_doesNotUpdateTheSameValue() {
        val key = "key"
        val value = Container.Success("value")
        every { lazyCache.get(key) } returns value

        lazyCache.updateWith(key) { Container.Success("value") }

        verify(exactly = 0) {
            lazyCache.updateWith(key, any<Container<String>>())
        }
    }

    @Test
    fun updateWith_updatesNewValue() {
        val key = "key"
        val oldValue = Container.Success("old-value")
        val newValue = Container.Success("new-value")
        every { lazyCache.get(key) } returns oldValue

        lazyCache.updateWith(key) { newValue }

        verify(exactly = 1) {
            lazyCache.updateWith(key, newValue)
        }
    }

    @Test
    fun createSimple_delegatesCallToCreateAndEmitsOneValue() = runTest {
        try {
            mockkObject(LazyCache.Companion)
            val loaderSlot = slot<CacheValueLoader<String, String>>()
            val emitter = mockk<Emitter<String>>(relaxUnitFun = true)
            val timeoutMillis = 2000L
            val dispatcher = mockk<CoroutineDispatcher>()
            every { LazyCache.create<String, String>(any(), any(), any()) } returns lazyCache

            val newLazyCache = LazyCache.createSimple<String, String>(
                loadingDispatcher = dispatcher,
                cacheTimeoutMillis = timeoutMillis,
            ) { arg -> "$arg-loaded" }

            assertSame(lazyCache, newLazyCache)
            verify(exactly = 1) {
                LazyCache.create(
                    cacheTimeoutMillis = timeoutMillis,
                    loadingDispatcher = refEq(dispatcher),
                    loader = capture(loaderSlot)
                )
            }
            val loaderFunction = loaderSlot.captured
            loaderFunction.invoke(emitter, "arg123")
            coVerify(exactly = 1) {
                emitter.emit("arg123-loaded")
            }
        } finally {
            unmockkObject(LazyCache.Companion)
        }
    }

    @Test
    fun updateIfSuccess_withNonSuccessPreviousValue_doesNothing() {
        val pending = Container.Pending
        val error = Container.Error(IllegalStateException(""))
        every { lazyCache.get("arg") } returns pending andThen error

        // test pending
        lazyCache.updateIfSuccess("arg") { "value" }
        verify(exactly = 0) {
            lazyCache.updateWith(any(), any<Container<String>>())
        }

        // test error
        lazyCache.updateIfSuccess("arg") { "value" }
        verify(exactly = 0) {
            lazyCache.updateWith(any(), any<Container<String>>())
        }
    }

    @Test
    fun updateIfSuccess_withSuccessPreviousValue_updatesValue() {
        val oldValue = "value"
        every { lazyCache.get("arg") } returns Container.Success(oldValue)

        lazyCache.updateIfSuccess("arg") { "updated-$it" }

        verify(exactly = 1) {
            lazyCache.updateWith("arg", Container.Success("updated-value"))
        }
    }

}