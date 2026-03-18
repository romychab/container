package com.elveum.container.demo.navigation.examples

import androidx.compose.runtime.Composable
import com.elveum.container.demo.feature.examples.container_metadata.ContainerMetadataScreen
import com.elveum.container.demo.feature.examples.container_of.ContainerOfScreen
import com.elveum.container.demo.feature.examples.container_states.ContainerStatesScreen
import com.elveum.container.demo.feature.examples.container_transformations.ContainerTransformationsScreen
import com.elveum.container.demo.feature.examples.container_unwrapping.ContainerUnwrapScreen
import com.elveum.container.demo.feature.examples.container_values.ContainerValuesScreen
import com.elveum.container.demo.feature.examples.reducer_container.FlowToContainerReducerScreen
import com.elveum.container.demo.feature.examples.reducer_container_flow.ContainerFlowToContainerReducerScreen
import com.elveum.container.demo.feature.examples.reducer_owner.ReducerOwnerScreen
import com.elveum.container.demo.feature.examples.reducer_pattern.FlowToReducerScreen
import com.elveum.container.demo.feature.examples.reducer_state_pattern.ReducerPrivatePublicStatePatternScreen
import com.elveum.container.demo.feature.examples.subject_args.ArgsScreen
import com.elveum.container.demo.feature.examples.subject_chunks.ChunksScreen
import com.elveum.container.demo.feature.examples.subject_basics.SubjectBasicsScreen
import com.elveum.container.demo.feature.examples.pagination_basic.BasicPaginationScreen
import com.elveum.container.demo.feature.examples.subject_local_remote.LocalRemoteScreen
import com.elveum.container.demo.feature.examples.subject_errors.ErrorHandlingScreen
import com.elveum.container.demo.feature.examples.subject_pulltorefresh.PullToRefreshScreen
import kotlinx.serialization.Serializable

@Serializable
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

    @Serializable
    data object ReducerFromFlow : Example() {
        @Transient override val category = Category.ReducerPattern
        @Transient override val title = "Reducer Basics"
        @Transient override val description = "Reducer<T> builds the final state from an input flow and manual updates."

        @Composable
        override fun Content() = FlowToReducerScreen()
    }

    @Serializable
    data object ReducerOwner : Example() {
        @Transient override val category = Category.ReducerPattern
        @Transient override val title = "ReducerOwner"
        @Transient override val description = "Uses the ReducerOwner interface to simplify creating Reducer<T> instances by specifying the coroutine scope and sharing strategy once."

        @Composable
        override fun Content() = ReducerOwnerScreen()
    }

    @Serializable
    data object ReducerContainerFromFlow : Example() {
        @Transient override val category = Category.ReducerPattern
        @Transient override val title = "ContainerReducer"
        @Transient override val description = "Wraps a Flow<T> into ContainerReducer<T> using toContainerReducer() extension. Two independent updateState calls mutate color and stroke width without re-loading."

        @Composable
        override fun Content() = FlowToContainerReducerScreen()
    }

    @Serializable
    data object ContainerFlowToContainerReducer : Example() {
        @Transient override val category = Category.ReducerPattern
        @Transient override val title = "ContainerFlow to ContainerReducer"
        @Transient override val description = "Converts Flow<Container<T>> into ContainerReducer<T> using containerToReducer() extension. Merges live repository updates with user-driven filter."

        @Composable
        override fun Content() = ContainerFlowToContainerReducerScreen()
    }

    @Serializable
    data object PrivatePublicStatePattern : Example() {
        @Transient override val category = Category.ReducerPattern
        @Transient override val title = "Reducer Private-Public State"
        @Transient override val description = "Uses reducers and private-public ViewModel's state to expose only final properties to the screen. Converts a tree structure into a flattened, expandable list."

        @Composable
        override fun Content() = ReducerPrivatePublicStatePatternScreen()
    }

    @Serializable
    data object LazyFlowSubjectBasics : Example() {
        @Transient override val category = Category.LazyFlowSubject
        @Transient override val title = "LazyFlowSubject Basics"
        @Transient override val description = "Turns any suspending loader function into a reactive Flow using LazyFlowSubject."

        @Composable
        override fun Content() = SubjectBasicsScreen()
    }

    @Serializable
    data object LazySubjectLocalRemote : Example() {
        @Transient override val category = Category.LazyFlowSubject
        @Transient override val title = "Local and Remote Cache"
        @Transient override val description = "Emits cached local data immediately, then replaces it with fresh remote data. SourceType metadata indicates the origin of each emission."

        @Composable
        override fun Content() = LocalRemoteScreen()
    }

    @Serializable
    data object LazySubjectPullToRefresh : Example() {
        @Transient override val category = Category.LazyFlowSubject
        @Transient override val title = "Pull to Refresh"
        @Transient override val description = "Demonstrates silent reloading via LoadConfig.SilentLoading. Uses backgroundLoadState metadata to drive the pull-to-refresh indicator without hiding existing content."

        @Composable
        override fun Content() = PullToRefreshScreen()
    }


    @Serializable
    data object LazySubjectErrorHandling : Example() {
        @Transient override val category = Category.LazyFlowSubject
        @Transient override val title = "Error Handling"
        @Transient override val description = "Shows how Container enters the Error state on failure. Demonstrates silent reload from the action bar, disabled while a background load is in progress."

        @Composable
        override fun Content() = ErrorHandlingScreen()
    }


    @Serializable
    data object LazySubjectArgs : Example() {
        @Transient override val category = Category.LazyFlowSubject
        @Transient override val title = "Reactive Arguments"
        @Transient override val description = "Uses dependsOnFlow() to declare reactive loader arguments. The loader re-executes automatically when a dependency emits a new value."

        @Composable
        override fun Content() = ArgsScreen()
    }

    @Serializable
    data object LazySubjectChunks : Example() {
        @Transient override val category = Category.LazyFlowSubject
        @Transient override val title = "Parallel Loading"
        @Transient override val description = "Fetches mosaic basic info first, then loads all 144 pixel-art tiles in parallel. Each tile appears as soon as its data arrives."

        @Composable
        override fun Content() = ChunksScreen()
    }

    @Serializable
    data object PaginationBasics : Example() {
        @Transient override val category = Category.Pagination
        @Transient override val title = "Pagination Basics"
        @Transient override val description = "Uses pageLoader with a nullable initial key to fetch pages on demand. New pages load automatically as the user scrolls near the end of the list."

        @Composable
        override fun Content() = BasicPaginationScreen()
    }

}
