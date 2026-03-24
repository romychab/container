package com.elveum.container.demo.navigation.examples

object ExampleRegistry {

    val all: List<Example> = listOf(
        Example.ContainerStates,
        Example.ContainerOf,
        Example.ContainerUnwrapping,
        Example.ContainerValues,
        Example.ContainerMetadata,
        Example.ContainerTransformations,

        Example.ReducerFromFlow,
        Example.ReducerOwner,
        Example.ReducerContainerFromFlow,
        Example.ContainerFlowToContainerReducer,
        Example.PrivatePublicStatePattern,

        Example.LazyFlowSubjectBasics,
        Example.LazySubjectLocalRemote,
        Example.LazySubjectPullToRefresh,
        Example.LazySubjectErrorHandling,
        Example.LazySubjectArgs,
        Example.LazySubjectChunks,

        Example.PaginationBasics,
        Example.PaginationStatuses,
        Example.PaginationArgs,
    )

    val byCategory: Map<Category, List<Example>> = all.groupBy { it.category }

}
