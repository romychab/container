package com.elveum.container.factory

import com.elveum.container.Container
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.successContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerFactoryExtensionsTest {

    @Test
    fun createSimpleSubject_returnsSubjectThatEmitsLoadedValue() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val subject = factory.createSimpleSubject { "hello" }
        val result = subject.listen().first { it is Container.Success }
        assertEquals(successContainer("hello"), result.raw())
    }

    @Test
    fun createSimpleSubject_withSourceType_attachesSourceTypeToContainer() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val subject = factory.createSimpleSubject(sourceType = RemoteSourceType) { "data" }
        val result = subject.listen().first { it is Container.Success } as Container.Success
        assertEquals(RemoteSourceType, result.sourceType)
    }

    @Test
    fun createSimpleFlow_emitsSuccessContainer() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val flow = factory.createSimpleFlow { "hello" }
        val result = flow.first { it is Container.Success }
        assertEquals(successContainer("hello"), result.raw())
    }

    @Test
    fun createSimpleFlow_withSourceType_attachesSourceType() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val flow = factory.createSimpleFlow(sourceType = LocalSourceType) { "data" }
        val result = flow.first { it is Container.Success } as Container.Success
        assertEquals(LocalSourceType, result.sourceType)
    }

    @Test
    fun createSimpleCache_returnsCache() {
        val factory = DefaultSubjectFactory()
        val cache = factory.createSimpleCache<String, Int> { key -> key.length }
        assertNotNull(cache)
    }

    @Test
    fun createSimpleCache_createsEntriesByKey() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val cache = factory.createSimpleCache<String, Int> { key -> key.length }
        val result = cache.listen("hello").first { it is Container.Success }
        assertEquals(successContainer(5), result.raw())
    }

    @Test
    fun createSimpleCache_withSourceType_attachesSourceType() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val cache = factory.createSimpleCache<String, Int>(sourceType = RemoteSourceType) { key -> key.length }
        val result = cache.listen("hi").first { it is Container.Success } as Container.Success
        assertEquals(RemoteSourceType, result.sourceType)
    }

    @Test
    fun createFlow_emitsContainersFromLoader() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val flow = factory.createFlow<String> {
            emit("first", isLastValue = true)
        }
        val result = flow.first { it is Container.Success }
        assertEquals(successContainer("first"), result.raw())
    }

    @Test
    fun createReloadableFlow_emitsContainersFromLoader() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val flow = factory.createReloadableFlow<String> {
            emit("value", isLastValue = true)
        }
        val result = flow.first { it is Container.Success }
        assertTrue(result is Container.Success)
    }

    @Test
    fun createReloadableFlow_emittedContainerHasReloadFunction() = runTest {
        val factory = DefaultSubjectFactory(coroutineScopeFactory = CoroutineScopeFactory { TestScope(testScheduler) })
        val flow = factory.createReloadableFlow<String> {
            emit("value", isLastValue = true)
        }
        val result = flow.first { it is Container.Success } as Container.Success
        assertNotNull(result.reloadFunction)
    }

}
