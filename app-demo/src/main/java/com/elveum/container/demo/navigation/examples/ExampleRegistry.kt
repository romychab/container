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
    )

    val byCategory: Map<Category, List<Example>> = all.groupBy { it.category }

}