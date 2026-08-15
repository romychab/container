package com.elveum.container.subject

import com.elveum.container.errorContainer
import com.elveum.container.exceptionOrNull
import com.elveum.container.factory.DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS
import com.elveum.container.pendingContainer
import com.elveum.container.subject.transformation.LoaderDecorator
import com.elveum.container.successContainer
import com.elveum.container.utils.raw
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LazyFlowSubjectDecoratorIntegrationTest : AbstractLazyFlowSubjectIntegrationTest() {

    @Test
    fun loaderDecorator_wrapsOriginLoader() = runFlowTest {
        val sessionFlow = MutableStateFlow("token")
        val loaderDecorator = LoaderDecorator { loader ->
            val token = dependsOnFlow("getToken") { sessionFlow }
            // throw Exception when token does not exist
            if (token.isBlank()) throw Exception("No valid session")
            loader()
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        runCurrent()
        assertEquals(
            listOf(pendingContainer(), successContainer("item")),
            state.collectedItems.raw(),
        )

        sessionFlow.value = ""
        advanceTimeBy(DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS + 1)
        assertTrue(state.lastItem.exceptionOrNull()?.message == "No valid session")
    }

}