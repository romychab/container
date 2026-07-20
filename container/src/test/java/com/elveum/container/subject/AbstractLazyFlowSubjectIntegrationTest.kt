package com.elveum.container.subject

import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.subject.lazy.LoadTaskManager
import com.uandcode.flowtest.FlowTestScope
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope

internal abstract class AbstractLazyFlowSubjectIntegrationTest {

    protected val cacheTimeout = 1000L

    protected fun FlowTestScope.createLazyFlowSubject(
        loader: ValueLoader<String>? = null,
    ): LazyFlowSubjectImpl<String> = createLazyFlowSubject(
        metadata = EmptyMetadata,
        loader = loader,
    )

    protected fun FlowTestScope.createLazyFlowSubject(
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