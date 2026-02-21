@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.IsLoadingInBackgroundMetadata
import com.elveum.container.LoadUuidMetadata
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.get
import com.elveum.container.isLoadingInBackground
import com.elveum.container.sourceType
import com.elveum.container.subject.FlowSubject
import com.elveum.container.successContainer
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FlowEmitterTest {

    @MockK
    private lateinit var flowCollector: FlowCollector<Container<String>>

    @MockK
    private lateinit var flowSubject: FlowSubject<String>

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

        flowEmitter.emit("item", LocalSourceType)

        coVerify(exactly = 1) {
            flowSubject.onNext("item")
            flowCollector.emit(successContainer("item", LocalSourceType, true))
        }
    }

    @Test
    fun emitLastItem_sendsLastItem() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", LocalSourceType)
        flowEmitter.emitLastItem()

        coVerify(exactly = 1) {
            flowSubject.onNext("item")
            flowCollector.emit(successContainer("item", LocalSourceType, true))
            flowCollector.emit(successContainer("item", LocalSourceType, false))
        }
    }

    @Test
    fun emit_withoutLoadUuid_doesNotAttachLoadUuidMetadata() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item")

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>() == null
            })
        }
    }

    @Test
    fun emit_withBlankLoadUuid_doesNotAttachLoadUuidMetadata() = runTest {
        val flowEmitter = makeFlowEmitter(loadUuid = "  ")

        flowEmitter.emit("item")

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>() == null
            })
        }
    }

    @Test
    fun emit_withNonBlankLoadUuid_attachesLoadUuidMetadataToEmittedContainer() = runTest {
        val flowEmitter = makeFlowEmitter(loadUuid = "test-uuid")

        flowEmitter.emit("item")

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>() == LoadUuidMetadata("test-uuid")
            })
        }
    }

    @Test
    fun emit_whenNotLastValue_attachesIsLoadingInBackgroundTrue() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", isLastValue = false)

        coVerify {
            flowCollector.emit(match { container ->
                container.metadata.isLoadingInBackground
            })
        }
    }

    @Test
    fun emit_whenLastValue_attachesIsLoadingInBackgroundFalse() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", isLastValue = true)

        coVerify {
            flowCollector.emit(match { container ->
                !container.metadata.isLoadingInBackground
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
    fun emitLastItem_withNonBlankLoadUuid_attachesLoadUuidMetadataToReemittedContainer() = runTest {
        val flowEmitter = makeFlowEmitter(loadUuid = "test-uuid")

        flowEmitter.emit("item") // isLastValue=false by default, stores item for re-emit
        flowEmitter.emitLastItem()

        // Only the re-emitted container (from emitLastItem) has isLoadingInBackground=false
        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>() == LoadUuidMetadata("test-uuid") &&
                    !container.metadata.isLoadingInBackground
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
                    !container.metadata.isLoadingInBackground
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
    fun emit_withEmitArgMetadata_overridesBuiltInLoadUuidMetadata() = runTest {
        val flowEmitter = makeFlowEmitter(loadUuid = "built-in-uuid")

        flowEmitter.emit("item", metadata = LoadUuidMetadata("arg-uuid"))

        coVerify(exactly = 0) {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>()?.uuid == "built-in-uuid"
            })
        }
        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>()?.uuid == "arg-uuid"
            })
        }
    }

    @Test
    fun emit_withEmitArgMetadata_overridesBuiltInIsLoadingInBackgroundMetadata() = runTest {
        val flowEmitter = makeFlowEmitter()

        // isLastValue=false would normally produce isLoadingInBackground=true,
        // but the emit arg overrides it to false
        flowEmitter.emit("item", IsLoadingInBackgroundMetadata(false), isLastValue = false)

        coVerify {
            flowCollector.emit(match { container ->
                !container.metadata.isLoadingInBackground
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
    fun emit_withConstructorMetadata_overridesBuiltInLoadUuidMetadata() = runTest {
        val flowEmitter = makeFlowEmitter(
            metadata = LoadUuidMetadata("constructor-uuid"),
            loadUuid = "built-in-uuid",
        )

        flowEmitter.emit("item")

        coVerify(exactly = 0) {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>()?.uuid == "built-in-uuid"
            })
        }
        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container.metadata.get<LoadUuidMetadata>()?.uuid == "constructor-uuid"
            })
        }
    }

    @Test
    fun emit_withConstructorMetadata_overridesBuiltInIsLoadingInBackgroundMetadata() = runTest {
        // isLastValue=false would normally produce isLoadingInBackground=true
        val flowEmitter = makeFlowEmitter(metadata = IsLoadingInBackgroundMetadata(false))

        flowEmitter.emit("item", isLastValue = false)

        coVerify {
            flowCollector.emit(match { container ->
                !container.metadata.isLoadingInBackground
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
                    !container.metadata.isLoadingInBackground
            })
        }
    }

    private fun makeFlowEmitter(
        metadata: ContainerMetadata = EmptyMetadata,
        loadUuid: String = "",
    ) = FlowEmitter(
        metadata = metadata,
        flowCollector = flowCollector,
        executeParams = LoadTask.ExecuteParams(loadUuid = loadUuid),
        flowSubject = flowSubject,
    )
}
