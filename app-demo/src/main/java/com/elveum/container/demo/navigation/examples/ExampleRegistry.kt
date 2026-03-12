package com.elveum.container.demo.navigation.examples

object ExampleRegistry {

    val all: List<Example> = listOf(
        Example.ContainerStates,
        Example.ContainerOf,
        Example.ContainerUnwrapping,
    )

    val byCategory: Map<Category, List<Example>> = all.groupBy { it.category }

}