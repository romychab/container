package com.elveum.store.demo.navigation.examples

import androidx.compose.runtime.Composable
import com.elveum.store.demo.feature.examples.store_keyed.basic.BasicListScreen
import com.elveum.store.demo.feature.examples.store_keyed.master_details.list.MasterListScreen
import com.elveum.store.demo.feature.examples.store_keyed.shopping.products.ProductsScreen
import com.elveum.store.demo.feature.examples.store_paged.pagination_args.PaginationArgsScreen
import com.elveum.store.demo.feature.examples.store_paged.pagination_basic.BasicPaginationScreen
import com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote.LocalRemotePagedScreen
import com.elveum.store.demo.feature.examples.store_paged.pagination_statuses.PaginationStatusesScreen
import com.elveum.store.demo.feature.examples.store_paged.pagination_updates.PaginationUpdatesScreen
import com.elveum.store.demo.feature.examples.store_simple.args.ArgsSimpleStoreScreen
import com.elveum.store.demo.feature.examples.store_simple.basic.SimpleStoreBasicsScreen
import com.elveum.store.demo.feature.examples.store_simple.combined.CombinedSimpleStoreScreen
import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.LocalRemoteReactiveSimpleStoreScreen
import com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending.LocalRemoteSuspendingSimpleStoreScreen
import com.elveum.store.demo.feature.examples.store_simple.pull_to_refresh.PullToRefreshSimpleStoreScreen
import kotlinx.serialization.Serializable

@Serializable
sealed class Example {

    abstract val title: String
    abstract val category: Category
    abstract val description: String

    @Composable
    abstract fun Content()

    @Serializable
    data object BasicSimpleStore : Example() {
        @Transient override val category = Category.SimpleStore
        @Transient override val title = "Basic Store"
        @Transient override val description = "Load, reload, edit data with SimpleStore, display 3 states: loading, success, and error"

        @Composable
        override fun Content() = SimpleStoreBasicsScreen()
    }

    @Serializable
    data object PullToRefreshSimpleStore : Example() {
        @Transient override val category = Category.SimpleStore
        @Transient override val title = "Pull to Refresh"
        @Transient override val description = "Refresh data silently keeping old content visible to the user."

        @Composable
        override fun Content() = PullToRefreshSimpleStoreScreen()
    }

    @Serializable
    data object LocalRemoteSuspendingSimpleStore : Example() {
        @Transient override val category = Category.SimpleStore
        @Transient override val title = "Local Storage Cache"
        @Transient override val description = "Load values both from local and remote data source."

        @Composable
        override fun Content() = LocalRemoteSuspendingSimpleStoreScreen()
    }
    @Serializable
    data object LocalRemoteReactiveSimpleStore : Example() {
        @Transient override val category = Category.SimpleStore
        @Transient override val title = "Reactive Local Storage Cache"
        @Transient override val description = "Update values immediately when data is changed in a reactive local storage."

        @Composable
        override fun Content() = LocalRemoteReactiveSimpleStoreScreen()
    }

    @Serializable
    data object ArgsSimpleStore : Example() {
        @Transient override val category = Category.SimpleStore
        @Transient override val title = "Simple Store Args"
        @Transient override val description = "Observe data using additional keys/filters."

        @Composable
        override fun Content() = ArgsSimpleStoreScreen()
    }

    @Serializable
    data object CombinedSimpleStore : Example() {
        @Transient override val category = Category.SimpleStore
        @Transient override val title = "All Together"
        @Transient override val description = "Fetch, cache, refresh, and update everything at once."

        @Composable
        override fun Content() = CombinedSimpleStoreScreen()
    }

    @Serializable
    data object BasicKeyedStore : Example() {
        @Transient override val category = Category.KeyedStore
        @Transient override val title = "Basic Keyed Store"
        @Transient override val description = "Observe a list while each item's description loads in parallel from a KeyedStore."

        @Composable
        override fun Content() = BasicListScreen()
    }

    @Serializable
    data object MasterDetailsKeyedStore : Example() {
        @Transient override val category = Category.KeyedStore
        @Transient override val title = "Master Details"
        @Transient override val description = "Observe the list of items and their details on click."

        @Composable
        override fun Content() = MasterListScreen()
    }

    @Serializable
    data object ShoppingCartKeyedStore : Example() {
        @Transient override val category = Category.KeyedStore
        @Transient override val title = "Shopping Cart"
        @Transient override val description = "Browse a product grid backed by a store and manage a shared cart on a second screen."

        @Composable
        override fun Content() = ProductsScreen()
    }

    @Serializable
    data object PaginationBasics : Example() {
        @Transient override val category = Category.PagedStore
        @Transient override val title = "Pagination Basics"
        @Transient override val description = "Uses PagedStore with an initial key to fetch pages on demand. New pages load automatically as the user scrolls near the end of the list."

        @Composable
        override fun Content() = BasicPaginationScreen()
    }

    @Serializable
    data object PaginationStatuses : Example() {
        @Transient override val category = Category.PagedStore
        @Transient override val title = "Pagination Statuses"
        @Transient override val description = "Combine pull-to-refresh, incremental paging, and error handling. A checkbox toggles simulated failures to demonstrate all error states."

        @Composable
        override fun Content() = PaginationStatusesScreen()
    }

    @Serializable
    data object PaginationArgs : Example() {
        @Transient override val category = Category.PagedStore
        @Transient override val title = "Pagination with Args"
        @Transient override val description = "Adds a category filter as an argument via queryFilter. Toggling a chip restarts paging from the first page with the new filter applied."

        @Composable
        override fun Content() = PaginationArgsScreen()
    }

    @Serializable
    data object PaginationUpdates : Example() {
        @Transient override val category = Category.PagedStore
        @Transient override val title = "Pagination with Updates"
        @Transient override val description = "Paged list where each item carries a Like toggle that can be flipped independently of pagination."

        @Composable
        override fun Content() = PaginationUpdatesScreen()
    }

    @Serializable
    data object PaginationLocalRemote : Example() {
        @Transient override val category = Category.PagedStore
        @Transient override val title = "Pagination with 2 Data Sources"
        @Transient override val description = "Paged list where each item is fetched from a local cache and remote source."

        @Composable
        override fun Content() = LocalRemotePagedScreen()
    }

}
