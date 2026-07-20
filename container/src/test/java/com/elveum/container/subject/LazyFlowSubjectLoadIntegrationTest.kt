package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Container.Pending
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.LoadTrigger
import com.elveum.container.RemoteSourceType
import com.elveum.container.ReplaceErrorsOnReload
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.get
import com.elveum.container.pendingContainer
import com.elveum.container.sourceType
import com.elveum.container.successContainer
import com.elveum.container.utils.invokeOn
import com.elveum.container.utils.raw
import com.uandcode.flowtest.CollectStatus
import com.uandcode.flowtest.runFlowTest
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.spyk
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Test.None

internal class LazyFlowSubjectLoadIntegrationTest : AbstractLazyFlowSubjectIntegrationTest() {

    @Test
    fun newLoad_startsNewLoadWithPendingStatus() = runFlowTest {
        val loader1: ValueLoader<String> = ValueLoader { emit("111") }
        val loader2: ValueLoader<String> = ValueLoader {
            delay(100)
            emit("222")
        }
        val spyLoader2 = spyk(loader2)
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()
        subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, spyLoader2)
        runCurrent()

        coVerify(exactly = 1) { spyLoader2.invokeOn(any()) }
        assertEquals(
            listOf(Pending, successContainer("111"), Pending),
            collectedItems.raw()
        )
    }

    @Test
    fun newLoad_loadsNewValue() = runFlowTest {
        val loader1: ValueLoader<String> = ValueLoader { emit("111") }
        var loader2Calls = 0
        val loader2: ValueLoader<String> = ValueLoader {
            loader2Calls++
            delay(100)
            emit("222")
        }
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()
        subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, loader2)
        runCurrent()
        advanceTimeBy(101)

        assertEquals(1, loader2Calls)
        assertEquals(
            listOf(Pending, successContainer("111"), Pending, successContainer("222")),
            collectedItems.raw()
        )
    }

    @Test
    fun newLoad_withSilentMode_loadsNewValueWithoutPendingStatus() = runFlowTest {
        val loader1: ValueLoader<String> = ValueLoader { emit("111") }
        var loader2Calls = 0
        val loader2: ValueLoader<String> = ValueLoader {
            loader2Calls++
            delay(100)
            emit("222")
        }
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()
        subject.newLoad(config = LoadConfig.SilentLoading, EmptyMetadata, loader2)
        runCurrent()
        advanceTimeBy(101)

        assertEquals(1, loader2Calls)
        assertEquals(
            listOf(Pending, successContainer("111"), successContainer("222")),
            collectedItems.raw()
        )
    }

    @Test
    fun newLoad_cancelsPreviousLoad() = runFlowTest {
        val loader1: ValueLoader<String> = ValueLoader {
            delay(100)
            emit("111")
        }
        val loader2: ValueLoader<String> = ValueLoader {
            delay(100)
            emit("222")
        }
        val spyLoader1 = spyk(loader1)
        val spyLoader2 = spyk(loader2)
        val subject = createLazyFlowSubject(spyLoader1)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(50)
        subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, spyLoader2)
        advanceTimeBy(101)

        coVerifyOrder {
            spyLoader1.invokeOn(any())
            spyLoader2.invokeOn(any())
        }
        assertEquals(
            listOf(Pending, successContainer("222")),
            collectedItems.raw()
        )
    }

    @Test
    fun newLoad_returnsFlowThatEmitsAllValuesOfNewUpload() = runFlowTest {
        val loader1: ValueLoader<String> = ValueLoader {
            emit("11")
            emit("12")
        }
        val loader2: ValueLoader<String> = ValueLoader {
            emit("21")
            emit("22")
        }
        val loader3: ValueLoader<String> = ValueLoader {
            emit("31")
            emit("32")
        }
        val subject = createLazyFlowSubject(loader1)

        subject.listen().startCollecting()
        runCurrent()
        val state1 = subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, loader2).startCollecting()
        runCurrent()
        val state2 = subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, loader3).startCollecting()
        runCurrent()

        assertEquals(listOf("21", "22"), state1.collectedItems)
        assertEquals(listOf("31", "32"), state2.collectedItems)
        assertEquals(CollectStatus.Completed, state1.collectStatus)
        assertEquals(CollectStatus.Completed, state2.collectStatus)
    }

    @Test
    fun newLoad_withCancelledLoad_returnsFlowThatFailsWithException() = runFlowTest {
        val defaultLoader: ValueLoader<String> = ValueLoader {
            emit("1")
        }
        val loader1: ValueLoader<String> = ValueLoader {
            emit("21")
            delay(100)
            emit("22")
        }
        val loader2: ValueLoader<String> = ValueLoader {
            emit("3")
        }
        val subject = createLazyFlowSubject(defaultLoader)

        subject.listen().startCollecting()
        runCurrent()
        val state1 = subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, loader1).startCollecting()
        advanceTimeBy(50)
        val state2 = subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, loader2).startCollecting()
        runCurrent()

        assertEquals(listOf("21"), state1.collectedItems)
        assertEquals(listOf("3"), state2.collectedItems)
        assertTrue(state1.collectStatus is CollectStatus.Cancelled)
        assertEquals(CollectStatus.Completed, state2.collectStatus)
    }

    @Test
    fun newLoad_withFailedLoad_returnsFlowThatAlsoFails() = runFlowTest {
        val defaultLoader: ValueLoader<String> = ValueLoader {
            emit("1")
        }
        val loader1: ValueLoader<String> = ValueLoader {
            emit("2")
            throw IllegalArgumentException()
        }
        val subject = createLazyFlowSubject(defaultLoader)

        subject.listen().startCollecting()
        runCurrent()
        val state = subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, loader1).startCollecting()
        runCurrent()

        assertEquals(listOf("2"), state.collectedItems)
        assertTrue((state.collectStatus as CollectStatus.Failed).exception is IllegalArgumentException)
    }

    @Test
    fun newLoad_withFailedLoad_emitsErrorToFlowReturnedByListenMethod() = runFlowTest {
        val loader1: ValueLoader<String> = ValueLoader { delay(100) }
        val loader2: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("222")
            delay(10)
            throw IllegalArgumentException()
        }
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()
        subject.newLoad(config = LoadConfig.Normal, EmptyMetadata, loader2)
        advanceTimeBy(11)

        assertEquals(
            listOf(Pending, successContainer("222")),
            collectedItems.raw()
        )
        advanceTimeBy(10)
        assertEquals(3, collectedItems.size)
        assertTrue((collectedItems[2] as Container.Error).exception is IllegalArgumentException)
    }

    @Test
    fun newLoad_afterNewLoad_usesLastLoader() = runFlowTest {
        val loader1: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("111")
        }
        val loader2: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("222")
        }
        val subject = createLazyFlowSubject(loader1)

        val state1 = subject.listen().startCollecting()
        advanceTimeBy(11)
        subject.newLoad(valueLoader = loader2)
        advanceTimeBy(11)
        state1.cancel()
        advanceTimeBy(cacheTimeout + 1)
        val state2 = subject.listen().startCollecting()
        advanceTimeBy(11)

        assertEquals(
            listOf(Pending, successContainer("111"), Pending, successContainer("222")),
            state1.collectedItems.raw()
        )
        assertEquals(
            listOf(Pending, successContainer("222")),
            state2.collectedItems.raw()
        )
    }

    @Test
    fun newLoad_withCustomMetadata_propagatesMetadataToValueLoader() = runFlowTest {
        val subject = createLazyFlowSubject { emit("initial") }
        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()

        subject.newLoad(metadata = SourceTypeMetadata(RemoteSourceType)) {
            val sourceType = metadata.sourceType
            emit(if (sourceType == RemoteSourceType) "remote" else "unknown")
        }
        runCurrent()

        assertEquals(
            listOf(Pending, successContainer("initial"), Pending, successContainer("remote")),
            collectedItems.raw()
        )
        assertEquals(RemoteSourceType, collectedItems.last().metadata.sourceType)
    }

    @Test(expected = None::class)
    fun reload_withoutPrevLoader_doesNothing() = runFlowTest {
        val subject = createLazyFlowSubject()

        val state = subject.listen().startCollecting()
        runCurrent()
        subject.reload()
        runCurrent()

        assertEquals(
            listOf(Pending),
            state.collectedItems,
        )
    }

    @Test
    fun reload_withPrevLoader_executesLoaderAgain() = runFlowTest {
        val subject = createLazyFlowSubject {
            if (loadTrigger == LoadTrigger.NewLoad) {
                emit("load")
            } else if (loadTrigger == LoadTrigger.Reload) {
                delay(10)
                emit("reload")
            }
        }

        val state = subject.listen().startCollecting()
        runCurrent()
        subject.reload()
        advanceTimeBy(11)

        assertEquals(
            listOf(pendingContainer(), successContainer("load"), pendingContainer(), successContainer("reload")),
            state.collectedItems.raw(),
        )
    }

    @Test
    fun reload_withCustomMetadata_propagatesMetadataToValueLoader() = runFlowTest {
        val subject = createLazyFlowSubject {
            val sourceType = metadata.get<SourceTypeMetadata>()?.sourceType
            emit(if (sourceType == RemoteSourceType) "remote" else "initial")
        }
        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()
        subject.reload(metadata = SourceTypeMetadata(RemoteSourceType))
        runCurrent()

        assertEquals(
            listOf(Pending, successContainer("initial"), Pending, successContainer("remote")),
            collectedItems.raw()
        )
        assertEquals(RemoteSourceType, collectedItems.last().metadata.sourceType)
    }

    @Test
    fun reload_withSilentLoading_whenCurrentIsError_keepsErrorInsteadOfPending() = runFlowTest {
        val subject = createLazyFlowSubject()
        subject.newAsyncLoad(config = LoadConfig.SilentLoading, EmptyMetadata, ValueLoader {
            if (loadTrigger == LoadTrigger.Reload) {
                delay(10)
                emit("reloaded")
            } else {
                throw IllegalStateException("boom")
            }
        })

        val state = subject.listen().startCollecting()
        runCurrent()
        assertTrue(state.lastItem is Container.Error)

        subject.reloadAsync()
        runCurrent()
        // SilentLoading without ReplaceErrorsOnReload keeps the stale error visible while reloading
        assertTrue(state.lastItem is Container.Error)

        advanceTimeBy(11)
        assertEquals(successContainer("reloaded"), state.lastItem.raw())
    }

    @Test
    fun reload_withReplaceErrorsOnReload_whenCurrentIsError_emitsPending() = runFlowTest {
        val subject = createLazyFlowSubject()
        subject.newAsyncLoad(
            config = LoadConfig.SilentLoading + ReplaceErrorsOnReload,
            EmptyMetadata,
            ValueLoader {
                if (loadTrigger == LoadTrigger.Reload) {
                    delay(10)
                    emit("reloaded")
                } else {
                    throw IllegalStateException("boom")
                }
            },
        )

        val state = subject.listen().startCollecting()
        runCurrent()
        assertTrue(state.lastItem is Container.Error)

        subject.reloadAsync()
        runCurrent()
        // ReplaceErrorsOnReload replaces the stale error with Pending while reloading
        assertEquals(Pending, state.lastItem)

        advanceTimeBy(11)
        assertEquals(successContainer("reloaded"), state.lastItem.raw())
    }

    @Test
    fun reload_withNullConfig_inheritsLastLoadConfig() = runFlowTest {
        val subject = createLazyFlowSubject()
        subject.newAsyncLoad(config = LoadConfig.SilentLoading, EmptyMetadata, ValueLoader {
            if (loadTrigger == LoadTrigger.Reload) {
                delay(10)
                emit("v2")
            } else {
                emit("v1")
            }
        })

        val state = subject.listen().startCollecting()
        runCurrent()
        assertEquals(successContainer("v1"), state.lastItem.raw())

        // reload without an explicit config -> inherits the previous load's SilentLoading config
        subject.reloadAsync(config = null)
        runCurrent()
        // still visible (not Pending) because the inherited config is silent
        assertEquals(successContainer("v1"), state.lastItem.raw())

        advanceTimeBy(11)
        assertEquals(successContainer("v2"), state.lastItem.raw())
        // no Pending was ever emitted after the first successful value
        val afterFirstSuccess = state.collectedItems.raw().dropWhile { it != successContainer("v1") }
        assertFalse(afterFirstSuccess.contains(Pending))
    }

}