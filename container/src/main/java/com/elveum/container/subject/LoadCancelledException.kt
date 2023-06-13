package com.elveum.container.subject

import kotlinx.coroutines.CancellationException

/**
 * A flow returned by [LazyFlowSubject.newLoad] may complete with
 * this exception if a new load has been submitted before the current
 * load finishes.
 */
class LoadCancelledException : CancellationException()

