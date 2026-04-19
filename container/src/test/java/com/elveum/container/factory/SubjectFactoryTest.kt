package com.elveum.container.factory

import com.elveum.container.cache.LazyCache
import com.elveum.container.subject.LazyFlowSubject
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class SubjectFactoryTest {

    @After
    fun tearDown() {
        SubjectFactory.resetFactory()
    }

    @Test
    fun setFactory_replacesDefaultFactory() {
        val customFactory = mockk<SubjectFactory>()
        val expectedSubject = mockk<LazyFlowSubject<String>>()
        every { customFactory.createSubject<String>(any(), any(), any(), any(), any()) } returns expectedSubject

        SubjectFactory.setFactory(customFactory)

        val result = SubjectFactory.createSubject<String> { /* loader */ }
        assertSame(expectedSubject, result)
        verify(exactly = 1) { customFactory.createSubject<String>(any(), any(), any(), any(), any()) }
    }

    @Test
    fun resetFactory_restoresDefaultFactory() {
        val customFactory = mockk<SubjectFactory>()
        SubjectFactory.setFactory(customFactory)

        SubjectFactory.resetFactory()

        // After reset, createSubject should not delegate to the custom factory
        val result = SubjectFactory.createSubject<String> { /* loader */ }
        assertNotNull(result)
        verify(exactly = 0) { customFactory.createSubject<String>(any(), any(), any(), any(), any()) }
    }

    @Test
    fun createSubject_delegatesToCurrentFactory() {
        val customFactory = mockk<SubjectFactory>()
        val expectedSubject = mockk<LazyFlowSubject<String>>()
        every { customFactory.createSubject<String>(any(), any(), any(), any(), any()) } returns expectedSubject
        SubjectFactory.setFactory(customFactory)

        val result = SubjectFactory.createSubject<String> { /* loader */ }

        assertSame(expectedSubject, result)
    }

    @Test
    fun createCache_delegatesToCurrentFactory() {
        val customFactory = mockk<SubjectFactory>()
        val expectedCache = mockk<LazyCache<String, Int>>()
        every { customFactory.createCache<String, Int>(any(), any(), any(), any(), any()) } returns expectedCache
        SubjectFactory.setFactory(customFactory)

        val result = SubjectFactory.createCache<String, Int> { /* loader */ }

        assertSame(expectedCache, result)
    }

    @Test
    fun defaultSubjectFactory_createSubject_returnsLazyFlowSubject() {
        val factory = DefaultSubjectFactory()
        val subject = factory.createSubject<String> { /* loader */ }
        assertNotNull(subject)
    }

    @Test
    fun defaultSubjectFactory_createCache_returnsLazyCache() {
        val factory = DefaultSubjectFactory()
        val cache = factory.createCache<String, Int> { /* loader */ }
        assertNotNull(cache)
    }

    @Test
    fun defaultSubjectFactory_createSubjectWithCustomTimeout_usesCustomTimeout() {
        val factory = DefaultSubjectFactory()
        val subject = factory.createSubject<String>(cacheTimeoutMillis = 5000L) { /* loader */ }
        assertNotNull(subject)
    }

    @Test
    fun defaultSubjectFactory_createCacheWithCustomTimeout_usesCustomTimeout() {
        val factory = DefaultSubjectFactory()
        val cache = factory.createCache<String, Int>(cacheTimeoutMillis = 5000L) { /* loader */ }
        assertNotNull(cache)
    }
}
