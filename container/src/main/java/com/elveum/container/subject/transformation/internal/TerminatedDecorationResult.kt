package com.elveum.container.subject.transformation.internal

internal sealed class TerminatedDecorationResult {
    data object ClearCache : TerminatedDecorationResult()
    data class Failure(val exception: Exception) : TerminatedDecorationResult()
}
