package com.elveum.container.subject

import com.elveum.container.ContainerMetadata
import com.elveum.container.get
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.delay
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

internal class LazyFlowSubjectOneShotMetadataIntegrationTest : AbstractLazyFlowSubjectIntegrationTest() {

    @Test
    fun `one-shot metadata is not emitted on next reload`() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        runCurrent()

        subject.reloadAsync(metadata = TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        subject.reloadAsync()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `one-shot metadata with disabled flag is emitted on next reload`() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        runCurrent()

        subject.reloadAsync(metadata = TestDisabledOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestDisabledOneShotMetadata>())

        subject.reloadAsync()
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestDisabledOneShotMetadata>())
    }

    @Test
    fun `one-shot metadata is not emitted after cache expiration`() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        runCurrent()

        subject.reloadAsync(metadata = TestOneShotMetadata)
        runCurrent()

        collector.cancel()
        advanceTimeBy(cacheTimeout + 1)
        val newCollector = subject.listenReloadable().startCollecting()
        runCurrent()

        assertNull(newCollector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `one-shot metadata is retained for a new collector while the cached value is alive`() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        runCurrent()

        subject.reloadAsync(metadata = TestOneShotMetadata)
        runCurrent()

        // re-subscribe before the in-memory cache expires: the same cached container
        // is re-emitted, so its one-shot metadata is still attached.
        collector.cancel()
        advanceTimeBy(cacheTimeout - 1)
        val newCollector = subject.listenReloadable().startCollecting()
        runCurrent()

        assertNotNull(newCollector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `one-shot metadata is not emitted after instant update`() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(10)
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        runCurrent()

        subject.reloadAsync(metadata = TestOneShotMetadata)
        advanceTimeBy(9)
        subject.updateWith(successContainer("222", TestOneShotMetadata))
        advanceTimeBy(2)

        subject.reloadAsync()
        advanceTimeBy(11)

        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }


    @Test
    fun `one-shot metadata is not emitted on next reload from container`() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        runCurrent()

        collector.lastItem.reload(metadata = TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        collector.lastItem.reload()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `one-shot metadata is not emitted after cache expiration from container`() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        runCurrent()

        collector.lastItem.reload(metadata = TestOneShotMetadata)
        runCurrent()

        collector.cancel()
        advanceTimeBy(cacheTimeout + 1)
        val newCollector = subject.listenReloadable().startCollecting()
        runCurrent()

        assertNull(newCollector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `one-shot metadata is not emitted after instant update from container`() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(10)
            emit("111")
        }

        val collector = subject.listenReloadable().startCollecting()
        advanceTimeBy(11)

        collector.lastItem.reload(metadata = TestOneShotMetadata)
        advanceTimeBy(9)
        subject.updateWith(successContainer("222", TestOneShotMetadata))
        advanceTimeBy(2)

        collector.lastItem.reload()
        advanceTimeBy(11)

        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    private data object TestOneShotMetadata : ContainerMetadata, ContainerMetadata.OneShot

    private data object TestDisabledOneShotMetadata : ContainerMetadata, ContainerMetadata.OneShot {
        override val isOneShot: Boolean = false
    }

}