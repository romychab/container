package com.elveum.container.subject

import com.elveum.container.Container

public data class ContainerConfiguration(

    /**
     * Set to `true` if you want to receive background loading
     * indicator for [Container.Completed] instances. Background
     * loading occurs when you call either [Container.Completed.reload]
     * or [LazyFlowSubject.reload] with flag `silently = true`. Silent
     * reloading keeps the current loaded data while a new data is still being
     * loaded.
     */
    val emitBackgroundLoads: Boolean = false,

    /**
     * Set to `true` if you want to receive reload functions
     * within [Container.Completed] instances. You can
     * use [Container.Completed.reload] to re-fetch data
     * encapsulated by the container.
     */
    val emitReloadFunction: Boolean = false,

)
