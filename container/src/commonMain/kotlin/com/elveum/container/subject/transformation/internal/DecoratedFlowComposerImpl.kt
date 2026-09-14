package com.elveum.container.subject.transformation.internal

import com.elveum.container.FlowComposer
import com.elveum.container.subject.transformation.DecoratedFlowComposer

internal class DecoratedFlowComposerImpl(
    private val originComposer: FlowComposer,
) : DecoratedFlowComposer, FlowComposer by originComposer {

    override fun completeWithFailure(exception: Exception): Nothing {
        throw TerminatedDecorationException(
            TerminatedDecorationResult.Failure(exception)
        )
    }

    override fun completeWithCacheCleanUp(): Nothing {
        throw TerminatedDecorationException(
            TerminatedDecorationResult.ClearCache
        )
    }
}
