package com.elveum.container.demo.navigation.examples

import androidx.compose.runtime.Composable
import com.elveum.container.demo.feature.examples.container_metadata.ContainerMetadataScreen
import com.elveum.container.demo.feature.examples.container_of.ContainerOfScreen
import com.elveum.container.demo.feature.examples.container_states.ContainerStatesScreen
import com.elveum.container.demo.feature.examples.container_transformations.ContainerTransformationsScreen
import com.elveum.container.demo.feature.examples.container_unwrapping.ContainerUnwrapScreen
import com.elveum.container.demo.feature.examples.container_values.ContainerValuesScreen
import kotlinx.serialization.Serializable

@Serializable()
sealed class Example {

    abstract val title: String
    abstract val category: Category
    abstract val description: String

    @Composable
    abstract fun Content()

    @Serializable
    data object ContainerStates : Example() {
        @Transient override val category = Category.ContainerType
        @Transient override val title = "Container States"
        @Transient override val description = "Renders Pending, Success, and Error states with the fold() function. Switches between states using buttons."

        @Composable
        override fun Content() = ContainerStatesScreen()
    }

    @Serializable
    data object ContainerOf : Example() {
        @Transient override val category = Category.ContainerType
        @Transient override val title = "ContainerOf Loaders"
        @Transient override val description = "Uses the containerOf { ... } function to wrap any block of code into Container<T>."

        @Composable
        override fun Content() = ContainerOfScreen()
    }

    @Serializable
    data object ContainerUnwrapping : Example() {
        @Transient override val category = Category.ContainerType
        @Transient override val title = "Container Unwrapping"
        @Transient override val description = "Uses the unwrap() extension within containerOf { ... } when combining results from multiple containers."

        @Composable
        override fun Content() = ContainerUnwrapScreen()
    }

    @Serializable
    data object ContainerValues : Example() {
        @Transient override val category = Category.ContainerType
        @Transient override val title = "Container Values"
        @Transient override val description = "Uses the getOrNull() / exceptionOrNull() extensions to convert any container into nullable values."

        @Composable
        override fun Content() = ContainerValuesScreen()
    }

    @Serializable
    data object ContainerMetadata : Example() {
        @Transient override val category = Category.ContainerType
        @Transient override val title = "Container Metadata"
        @Transient override val description = "Reads / Writes built-in additional fields attached to Container<T> or even creates custom fields."

        @Composable
        override fun Content() = ContainerMetadataScreen()
    }

    @Serializable
    data object ContainerTransformations : Example() {
        @Transient override val category = Category.ContainerType
        @Transient override val title = "Container Transformations"
        @Transient override val description = "Demonstrates map, mapException, catch, recover, and other transformation functions."

        @Composable
        override fun Content() = ContainerTransformationsScreen()
    }

}
