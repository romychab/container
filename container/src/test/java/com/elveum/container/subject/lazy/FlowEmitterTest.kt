@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.BackgroundLoadState
import com.elveum.container.BackgroundLoadMetadata
import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.backgroundLoadState
import com.elveum.container.get
import com.elveum.container.sourceType
import com.elveum.container.subject.FlowSubject
import com.elveum.container.successContainer
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FlowEmitterTest {

    @MockK
    private lateinit var flowCollector: FlowCollector<Container<String>>

    @MockK
    private lateinit var flowSubject: FlowSubject<String>

    @MockK
    private lateinit var flowDependencyStore: FlowDependencyStore

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun hasEmittedItems_beforeEmittingItems_returnsFalse() {
        val flowEmitter = makeFlowEmitter()
        assertFalse(flowEmitter.hasEmittedValues)
    }

    @Test
    fun hasEmittedItems_afterEmittingItem_returnsTrue() = runTest {
        val flowEmitter = makeFlowEmitter()
        flowEmitter.emit("item")
        assertTrue(flowEmitter.hasEmittedValues)
    }

    @Test
    fun emit_sendsItem_toCollectorAndSubject() = runTest {
        val flowEmitter = makeFlowEmitter()
        val expectedMetadata = BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                SourceTypeMetadata(LocalSourceType)

        flowEmitter.emit("item", LocalSourceType)

        coVerify(exactly = 1) {
            flowSubject.onNext("item")
            flowCollector.emit(successContainer("item", expectedMetadata))
        }
    }

    @Test
    fun emitLastItem_sendsLastItem() = runTest {
        val flowEmitter = makeFlowEmitter()
        val loadingMetadata = BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                SourceTypeMetadata(LocalSourceType)
        val emptyMetadata = BackgroundLoadMetadata(BackgroundLoadState.Idle) +
                SourceTypeMetadata(LocalSourceType)

        flowEmitter.emit("item", LocalSourceType)
        flowEmitter.emitLastItem()

        coVerify(exactly = 1) {
            flowSubject.onNext("item")
            flowCollector.emit(successContainer("item", loadingMetadata))
            flowCollector.emit(successContainer("item", emptyMetadata))
        }
    }

    @Test
    fun emit_whenNotLastValue_attachesBackgroundLoadLoading() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", isLastValue = false)

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.backgroundLoadState == BackgroundLoadState.Loading
            })
        }
    }

    @Test
    fun emit_whenLastValue_attachesBackgroundLoadEmpty() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", isLastValue = true)

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.backgroundLoadState == BackgroundLoadState.Idle
            })
        }
    }

    @Test
    fun emit_withCustomMetadata_attachesMetadataToEmittedContainer() = runTest {
        val flowEmitter = makeFlowEmitter()
        val customMetadata = SourceTypeMetadata(LocalSourceType)

        flowEmitter.emit("item", customMetadata)

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.sourceType == LocalSourceType
            })
        }
    }

    @Test
    fun emitLastItem_withCustomMetadata_propagatesMetadataToReemittedContainer() = runTest {
        val flowEmitter = makeFlowEmitter()
        val customMetadata = SourceTypeMetadata(LocalSourceType)

        flowEmitter.emit("item", customMetadata, isLastValue = false)
        flowEmitter.emitLastItem()

        // Only the re-emitted container (from emitLastItem) has isLoadingInBackground=false
        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container.metadata.get<SourceTypeMetadata>()?.sourceType == LocalSourceType &&
                    container.metadata.backgroundLoadState == BackgroundLoadState.Idle
            })
        }
    }

    @Test
    fun emitLastItem_afterLastValueEmitted_doesNotEmitAnything() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", isLastValue = true)
        flowEmitter.emitLastItem()

        // emitLastItem is a no-op when the previous item was the last one
        coVerify(exactly = 1) { flowCollector.emit(any()) }
    }

    @Test
    fun emit_withEmitArgMetadata_overridesBuiltInBackgroundLoadMetadata() = runTest {
        val flowEmitter = makeFlowEmitter()

        // isLastValue=false would normally produce BackgroundLoad.Loading,
        // but the emit arg overrides it to Empty
        flowEmitter.emit("item", BackgroundLoadMetadata(BackgroundLoadState.Idle), isLastValue = false)

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.backgroundLoadState == BackgroundLoadState.Idle
            })
        }
    }

    @Test
    fun emit_withConstructorMetadata_isAttachedToEmittedContainer() = runTest {
        val flowEmitter = makeFlowEmitter(metadata = SourceTypeMetadata(RemoteSourceType))

        flowEmitter.emit("item")

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.sourceType == RemoteSourceType
            })
        }
    }

    @Test
    fun emit_withConstructorMetadata_overridesEmitArgMetadataOfSameType() = runTest {
        val flowEmitter = makeFlowEmitter(metadata = SourceTypeMetadata(RemoteSourceType))

        flowEmitter.emit("item", SourceTypeMetadata(LocalSourceType))

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.sourceType == RemoteSourceType
            })
        }
    }

    @Test
    fun emit_withConstructorMetadata_overridesBuiltInBackgroundLoadMetadata() = runTest {
        // isLastValue=false would normally produce BackgroundLoad.Loading
        val flowEmitter = makeFlowEmitter(metadata = BackgroundLoadMetadata(BackgroundLoadState.Idle))

        flowEmitter.emit("item", isLastValue = false)

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.backgroundLoadState == BackgroundLoadState.Idle
            })
        }
    }

    @Test
    fun emitLastItem_withConstructorMetadata_propagatesConstructorMetadataToReemittedContainer() = runTest {
        val flowEmitter = makeFlowEmitter(metadata = SourceTypeMetadata(RemoteSourceType))

        flowEmitter.emit("item") // isLastValue=false by default, stores item for re-emit
        flowEmitter.emitLastItem()

        // Both initial emit and re-emit have RemoteSourceType;
        // only the re-emitted container (from emitLastItem) has isLoadingInBackground=false
        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container.metadata.sourceType == RemoteSourceType &&
                    container.metadata.backgroundLoadState == BackgroundLoadState.Idle
            })
        }
    }

    @Test
    fun dependsOnFlow_delegatesCallToFlowDependencyStore() = runTest {
        coEvery {
            flowDependencyStore.dependsOn<String>("key", "varargKey", flow = any())
        } coAnswers {
            thirdArg<() -> Flow<Container<String>>>()
                .invoke()
                .filterIsInstance<Container.Success<String>>()
                .first()
                .value
        }
        val flowEmitter = makeFlowEmitter()
        val flow = MutableStateFlow("one")

        val result = flowEmitter.dependsOnFlow("key", "varargKey") { flow }

        coVerifySequence {
            flowDependencyStore.dependsOn<String>("key", "varargKey", flow = any())
        }
        assertEquals("one", result)
    }

    @Test
    fun dependsOnContainerFlow_delegatesCallToFlowDependencyStore() = runTest {
        coEvery {
            flowDependencyStore.dependsOn<String>("key", "varargKey", flow = any())
        } coAnswers {
            thirdArg<() -> Flow<Container<String>>>()
                .invoke()
                .filterIsInstance<Container.Success<String>>()
                .first()
                .value
        }
        val flowEmitter = makeFlowEmitter()
        val flow = MutableStateFlow<Container<String>>(successContainer("one"))

        val result = flowEmitter.dependsOnContainerFlow("key", "varargKey") { flow }

        coVerify {
            flowDependencyStore.dependsOn<String>("key", "varargKey", flow = any())
        }
        assertEquals("one", result)
    }

    private fun makeFlowEmitter(
        metadata: ContainerMetadata = EmptyMetadata,
    ) = FlowEmitter(
        metadata = metadata,
        flowCollector = flowCollector,
        executeParams = LoadTask.ExecuteParams(flowDependencyStore),
        flowSubject = flowSubject,
    )
}
