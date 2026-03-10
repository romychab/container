package com.elveum.container.demo.navigation.examples

object ExampleRegistry {

    val all: List<Example> = listOf(
    )

    val byCategory: Map<Category, List<Example>> = all.groupBy { it.category }

}