package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Container.Error
import com.elveum.container.Container.Pending
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.EmptyReloadFunction
import com.elveum.container.LoadTrigger
import com.elveum.container.IsReloadDependenciesMetadata
import com.elveum.container.ReloadFunction
import com.elveum.container.ReloadFunctionMetadata
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.factory.DefaultReloadDependenciesPeriodMillis
import com.elveum.container.BackgroundLoadState
import com.elveum.container.LoadConfig
import com.elveum.container.backgroundLoadState
import com.elveum.container.get
import com.elveum.container.pendingContainer
import com.elveum.container.reloadFunction
import com.elveum.container.sourceType
import com.elveum.container.subject.lazy.LoadTaskManager
import com.elveum.container.successContainer
import com.elveum.container.utils.invokeOn
import com.elveum.container.utils.raw
import com.uandcode.flowtest.CollectStatus
import com.uandcode.flowtest.FlowTestScope
import com.uandcode.flowtest.runFlowTest
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Test.None

class LazyFlowSubjectImplIntegrationTest {

    private val cacheTimeout = 1000L

    @Test
    fun listen_whileLoadingItems_emitsPendingStatus() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(1000)
        }

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()

        assertEquals(
            listOf(Pending),
            collectedItems
        )
    }

    @Test
    fun listen_withEmptyLoader_emitsIllegalStateException() = runFlowTest {
        val subject = createLazyFlowSubject { }

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()

        assertEquals(2, collectedItems.size)
        assertEquals(Pending, collectedItems[0])
        assertTrue((collectedItems[1] as Error).exception is IllegalStateException)
    }

    @Test
    fun listen_emitsItemsProducedByLoader() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
            delay(1)
            emit("222")
            delay(100)
            emit("333")
        }

        val collectedItems = subject.listen()
            .startCollecting(StandardTestDispatcher(scope.testScheduler))
            .collectedItems
        advanceTimeBy(2)

        assertEquals(
            listOf(Pending, successContainer("111"), successContainer("222")),
            collectedItems.raw()
        )
        advanceTimeBy(100)
        assertEquals(
            listOf(Pending, successContainer("111"), successContainer("222"), successContainer("333")),
            collectedItems.raw()
        )
    }

    @Test
    fun listen_withFailedLoad_emitsException() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
            delay(1)
            throw IllegalArgumentException()
        }

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(2)

        assertEquals(3, collectedItems.size)
        assertEquals(Pending, collectedItems[0])
        assertEquals(successContainer("111"), collectedItems[1].raw())
        assertTrue((collectedItems[2] as Error).exception is IllegalArgumentException)
    }

    @Test
    fun listen_withinTimeout_holdsLatestLoadedValue() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val jobState1 = subject.listen().startCollecting()
        runCurrent()
        jobState1.cancel()
        advanceTimeBy(millis = cacheTimeout - 1)
        val jobState2 = subject.listen().startCollecting()
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("v1")),
            jobState1.collectedItems.raw()
        )
        assertEquals(
            listOf(successContainer("v1")),
            jobState2.collectedItems.raw()
        )
        coVerify(exactly = 1) { spyLoader.invokeOn(any()) }
    }

    @Test
    fun listen_afterTimeout_startsLoadingFromScratch() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val jobState1 = subject.listen().startCollecting()
        runCurrent()
        jobState1.cancel()
        advanceTimeBy(millis = cacheTimeout + 1)
        val jobState2 = subject.listen().startCollecting()
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("v1")),
            jobState1.collectedItems.raw()
        )
        assertEquals(
            listOf(pendingContainer(), successContainer("v2")),
            jobState2.collectedItems.raw()
        )
        coVerify(exactly = 2) { spyLoader.invokeOn(any()) }
    }

    @Test
    fun listen_whichIsCalledTwice_startsLoadingOnlyOnce() = runFlowTest {
        var index = 0
        var loaderCalls = 0
        val loader: ValueLoader<String> = {
            loaderCalls++
            delay(100)
            emit("v${++index}")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems1 = subject.listen().startCollecting().collectedItems
        advanceTimeBy(50)
        val collectedItems2 = subject.listen().startCollecting().collectedItems
        advanceTimeBy(51)

        assertEquals(1, loaderCalls)
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems1.raw()
        )
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems2.raw()
        )
    }

    @Test
    fun listen_whichIsCalledTwiceOnSameInstance_startsLoadingOnlyOnce() = runFlowTest {
        var index = 0
        var loaderCalls = 0
        val loader: ValueLoader<String> = {
            loaderCalls++
            delay(100)
            emit("v${++index}")
        }
        val subject = createLazyFlowSubject(loader)

        val flow = subject.listen()
        val collectedItems1 = flow.startCollecting().collectedItems
        advanceTimeBy(50)
        val collectedItems2 = flow.startCollecting().collectedItems
        advanceTimeBy(51)

        assertEquals(1, loaderCalls)
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems1.raw()
        )
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems2.raw()
        )
    }

    @Test
    fun listen_withCancellationOfNotAllCollectors_holdsCachedValue() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val collectState1 = subject.listen().startCollecting()
        runCurrent()
        val collectState2 = subject.listen().startCollecting()
        runCurrent()
        collectState2.cancel()
        val collectState3 = subject.listen().startCollecting()
        runCurrent()

        coVerify(exactly = 1) { spyLoader.invokeOn(any()) }
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectState1.collectedItems.raw()
        )
        assertEquals(
            listOf(successContainer("v1")),
            collectState2.collectedItems.raw()
        )
        assertEquals(
            listOf(successContainer("v1")),
            collectState3.collectedItems.raw()
        )
    }

    @Test
    fun newLoad_startsNewLoadWithPendingStatus() = runFlowTest {
        val loader1: ValueLoader<String> = { emit("111") }
        val loader2: ValueLoader<String> = {
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
        val loader1: ValueLoader<String> = { emit("111") }
        var loader2Calls = 0
        val loader2: ValueLoader<String> = {
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
        val loader1: ValueLoader<String> = { emit("111") }
        var loader2Calls = 0
        val loader2: ValueLoader<String> = {
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
        val loader1: ValueLoader<String> = {
            delay(100)
            emit("111")
        }
        val loader2: ValueLoader<String> = {
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
        val loader1: ValueLoader<String> = {
            emit("11")
            emit("12")
        }
        val loader2: ValueLoader<String> = {
            emit("21")
            emit("22")
        }
        val loader3: ValueLoader<String> = {
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
        val defaultLoader: ValueLoader<String> = {
            emit("1")
        }
        val loader1: ValueLoader<String> = {
            emit("21")
            delay(100)
            emit("22")
        }
        val loader2: ValueLoader<String> = {
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
        val defaultLoader: ValueLoader<String> = {
            emit("1")
        }
        val loader1: ValueLoader<String> = {
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
        val loader1: ValueLoader<String> = { delay(100) }
        val loader2: ValueLoader<String> = {
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
        assertTrue((collectedItems[2] as Error).exception is IllegalArgumentException)
    }

    @Test
    fun newLoad_afterNewLoad_usesLastLoader() = runFlowTest {
        val loader1: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val loader2: ValueLoader<String> = {
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
    fun updateWith_cancelsLoadingAndEmitsNewValueImmediately() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(6)
        subject.updateWith(successContainer("222"))
        advanceTimeBy(6)

        assertEquals(
            listOf(pendingContainer(), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWith_emitsNewValue() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(11)
        subject.updateWith(successContainer("222"))
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("111"), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWithMapper_cancelsLoadingAndEmitsNewValueImmediately() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(6)
        subject.updateWith { successContainer("222") }
        advanceTimeBy(6)

        assertEquals(
            listOf(pendingContainer(), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWithMapper_emitsNewValue() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(11)
        subject.updateWith { successContainer("222") }
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("111"), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWithMapper_withReturningSameValue_doesNotReEmitIt() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(11)
        subject.updateWith { it }
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("111")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWith_updatesValueImmediately() = runFlowTest {
        val subject = createLazyFlowSubject()

        subject.listen().startCollecting()
        subject.updateWith(successContainer("123"))

        assertEquals(successContainer("123"), subject.currentValue().raw())
    }

    @Test
    fun updateWith_withoutListeners_doesNotUpdateValueImmediately() = runFlowTest {
        val subject = createLazyFlowSubject()

        subject.updateWith(successContainer("123"))

        assertEquals(Pending, subject.currentValue())
    }

    @Test
    fun updateWith_afterResubscribing_usesPreviousLoader() = runFlowTest {
        val subject = createLazyFlowSubject(SourceTypeMetadata(RemoteSourceType)) {
            emit("real-item")
        }
        val oldState = subject.listen().startCollecting()
        runCurrent()

        subject.updateWith(successContainer("updated-item"))
        oldState.cancel()
        advanceTimeBy(cacheTimeout + 1)
        val newState = subject.listen().startCollecting()
        runCurrent()

        assertEquals(
            listOf(Pending, successContainer("real-item")),
            newState.collectedItems.raw()
        )
        assertEquals(RemoteSourceType, newState.collectedItems.last().metadata.sourceType)
    }

    @Test
    fun activeCollectorsCount_withoutCollectors_returnsZero() = runFlowTest {
        val subject = createLazyFlowSubject()

        assertEquals(0, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with1ActiveCollectors_returns1() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        subject.listen().startCollecting()

        assertEquals(1, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with2Collectors_returns2() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        subject.listen().startCollecting()
        subject.listen().startCollecting()

        assertEquals(2, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with2CollectorsOnSameFlow_returns2() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        val flow = subject.listen()
        flow.startCollecting()
        flow.startCollecting()

        assertEquals(2, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_afterCancellation_decreasesNumberOfCollectors() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        val state1 = subject.listen().startCollecting() // 1 active collector
        val state2 = subject.listen().startCollecting() // 2 active collectors

        state1.cancel() // 1 active collector again
        assertEquals(1, subject.activeCollectorsCount)
        state2.cancel() // 0 active collectors
        assertEquals(0, subject.activeCollectorsCount)
    }

    @Test
    fun loader_withDependencies_observeAllDependencies() = runFlowTest {
        val reloadFunction = mockk<ReloadFunction>(relaxed = true)
        val dependencyA = MutableSharedFlow<Container<String>>()
        val dependencyB = MutableSharedFlow<String>()
        var isDependencyBCancelled = false
        val subject = createLazyFlowSubject {
            val s1 = dependsOnContainerFlow("a") { dependencyA }
            val s2 = dependsOnFlow("b") {
                dependencyB.onCompletion {
                    isDependencyBCancelled = it is CancellationException
                }
            }
            emit("$s1:$s2")
        }

        val state = subject.listen().startCollecting()
        runCurrent()

        // 1. check first value merged from dependencies is received:
        dependencyA.emit(successContainer("a1"))
        runCurrent()
        dependencyB.emit("b1")
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("a1:b1")),
            state.collectedItems.raw(),
        )

        // 2. check nothing changed (2 items as before), because recomposition
        //    starts after a delay:
        dependencyA.emit(successContainer("a2", ReloadFunctionMetadata(reloadFunction)))
        assertEquals(2, state.count)

        // 3. advance delay and check new updated item received:
        advanceTimeBy(DefaultReloadDependenciesPeriodMillis + 1)
        assertEquals(
            successContainer("a2:b1"),
            state.lastItem.raw()
        )

        // 4. new loader, without dependencyB -> it must be freed
        subject.newAsyncLoad {
            emit(dependsOnContainerFlow("a") { dependencyA })
        }
        runCurrent()
        assertTrue(isDependencyBCancelled)

        // 5. reload (not silently) -> dependencies must be reloaded
        subject.reloadAsync(config = LoadConfig.Normal)
        verify(exactly = 0) { reloadFunction(LoadConfig.SilentLoading) }
        runCurrent()
        verify(exactly = 1) { reloadFunction(LoadConfig.Normal) }

        // 6. reload (silently) -> dependencies must be reloaded
        subject.reloadAsync(config = LoadConfig.SilentLoading)
        runCurrent()
        verify(exactly = 1) { reloadFunction(LoadConfig.SilentLoading) }

        // 7. reload without dependencies
        clearMocks(reloadFunction)
        subject.reloadAsync(metadata = IsReloadDependenciesMetadata(false))
        runCurrent()
        verify(exactly = 0) { reloadFunction(any()) }
    }

    @Test
    fun listen_withEnabledConfiguration_appliesConfiguration() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(10)
            emit("test")
        }

        val noConfigCollector = subject.listen().startCollecting()
        val reloadFunctionEnabledCollector = subject.listen(
            ContainerConfiguration(emitReloadFunction = true)
        ).startCollecting()
        val bgLoadsEnabledCollector = subject.listen(
            ContainerConfiguration(emitBackgroundLoads = true)
        ).startCollecting()
        val allConfigEnabledCollector = subject.listenReloadable().startCollecting()
        advanceTimeBy(11)

        // 1. Assert no reload function in noConfig & bgLoadsConfig:
        assertEquals(EmptyReloadFunction, noConfigCollector.lastItem.metadata.reloadFunction)
        assertEquals(EmptyReloadFunction, bgLoadsEnabledCollector.lastItem.metadata.reloadFunction)

        // 2. Assert reload function exists in reloadFunConfig
        reloadFunctionEnabledCollector.lastItem.metadata.reloadFunction.invoke(LoadConfig.Normal)
        advanceTimeBy(10)
        assertEquals(pendingContainer(), reloadFunctionEnabledCollector.lastItem)
        advanceTimeBy(1)

        // 3. Assert reload function exists in allConfig
        allConfigEnabledCollector.lastItem.metadata.reloadFunction.invoke(LoadConfig.Normal)
        advanceTimeBy(10)
        assertEquals(pendingContainer(), allConfigEnabledCollector.lastItem)
        advanceTimeBy(1)

        // 4. Assert background loading status in all containers
        subject.reloadAsync(config = LoadConfig.SilentLoading)
        advanceTimeBy(10)
        assertEquals(BackgroundLoadState.Idle, noConfigCollector.lastItem.metadata.backgroundLoadState)
        assertEquals(BackgroundLoadState.Idle, reloadFunctionEnabledCollector.lastItem.metadata.backgroundLoadState)
        assertEquals(BackgroundLoadState.Loading, bgLoadsEnabledCollector.lastItem.metadata.backgroundLoadState)
        assertEquals(BackgroundLoadState.Loading, allConfigEnabledCollector.lastItem.metadata.backgroundLoadState)
    }

    private fun FlowTestScope.createLazyFlowSubject(
        loader: ValueLoader<String>? = null,
    ): LazyFlowSubjectImpl<String> = createLazyFlowSubject(
        metadata = EmptyMetadata,
        loader = loader,
    )

    private fun FlowTestScope.createLazyFlowSubject(
        metadata: ContainerMetadata,
        loader: ValueLoader<String>? = null,
    ): LazyFlowSubjectImpl<String> {
        val coroutineScopeFactory = mockk<CoroutineScopeFactory>()

        every { coroutineScopeFactory.createScope() } answers {
            TestScope(scope.testScheduler)
        }
        return LazyFlowSubjectImpl<String>(
            coroutineScopeFactory,
            cacheTimeout,
            LoadTaskManager(),
        ).apply {
            if (loader != null) {
                newAsyncLoad(config = LoadConfig.Normal, metadata, loader)
            }
        }
    }
}
