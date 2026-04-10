# Pagination

This page covers the built-in pagination support provided by the `PageLoader`
API. It lets you load data page-by-page on demand, with automatic state
tracking for loading indicators, error handling, and retry.

## Table of Contents

- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [How It Works](#how-it-works)
- [Notifying the Loader About Rendered Items](#notifying-the-loader-about-rendered-items)
- [Next Page State](#next-page-state)
- [Retry on Error](#retry-on-error)
- [Pull-to-Refresh](#pull-to-refresh)
- [Flow Dependencies (Filtering)](#flow-dependencies-filtering)
- [Updating Items in a Paged List](#updating-items-in-a-paged-list)
- [PageEmitter API](#pageemitter-api)
- [Full Example](#full-example)

## Overview

`PageLoader<Key, T>` is a special `ValueLoader` that can be passed to
`LazyFlowSubject.create`. It loads data in pages, where each page is
identified by a key (e.g. a page index or a cursor string). Pages are loaded
on demand as the user scrolls through the list.

Key features:

- Automatic concatenation of loaded pages into a single `List<T>`
- Built-in next-page state tracking (`Idle`, `Pending`, `Error`)
- Retry support for failed page loads
- Metadata for rendering loading/error indicators per page
- Support for flow dependencies via `dependsOnFlow` / `dependsOnContainerFlow`

## Basic Usage

Use the `pageLoader` function to create a `PageLoader`:

```kotlin
private val ordersPageLoader = pageLoader(
    initialKey = 0,
    itemId = Order::id,
) { pageIndex ->
    val orders = ordersDataSource.fetchOrders(pageIndex)
    emitPage(orders)
    if (orders.isNotEmpty()) emitNextKey(pageIndex + 1)
}

private val subject = LazyFlowSubject.create(
    valueLoader = ordersPageLoader,
)

fun listenOrders(): Flow<Container<List<Order>>> = subject.listenReloadable()
```

The `pageLoader` function signature:

```kotlin
fun <Key, T> pageLoader(
    initialKey: Key,
    itemId: (T) -> Any,
    fetchDistance: Int = 10,
    emitMetadata: Boolean = true,
    block: suspend PageEmitter<Key, T>.(Key) -> Unit,
): PageLoader<Key, T>
```

| Parameter | Description |
|-----------|-------------|
| `initialKey` | The key of the first page to load |
| `itemId` | A function that returns a stable unique identifier for each item; used internally to deduplicate items across page reloads |
| `fetchDistance` | How many items before the end of the currently loaded list should trigger loading of the next page (default: `10`) |
| `emitMetadata` | Whether to attach `NextPageStateMetadata` and `OnItemRenderedCallbackMetadata` to emitted containers (default: `true`) |
| `block` | The loader function, called once per page with the page key as the argument |

## How It Works

1. When a subscriber starts collecting the flow, the loader is called with
   `initialKey`
2. Inside the loader, call `emitPage(list)` to provide the data for the
   current page. The library concatenates all loaded pages and emits the
   combined list to subscribers
3. Call `emitNextKey(key)` to register the key of the next page. If you do
   not call `emitNextKey`, the loader assumes there are no more pages
4. The next page is loaded when the user scrolls to within `fetchDistance`
   items of the end of the currently loaded data (triggered by
   `onItemRendered`)

## Notifying the Loader About Rendered Items

The page loader needs to know which items are currently visible so it can
trigger loading the next page at the right time.

When `emitMetadata = true` (the default), each emitted container includes an
`OnItemRenderedCallbackMetadata` accessible via `metadata.onItemRendered`.
Call it from a `LaunchedEffect` inside your `LazyColumn`:

```kotlin
val container: Container<List<Order>> by viewModel.ordersFlow.collectAsState()

container.fold(
    onPending = { CircularProgressIndicator() },
    onError = { /* ... */ },
    onSuccess = { orders ->
        // Inside onSuccess, `metadata` is available directly as a receiver
        LazyColumn {
            itemsIndexed(orders) { index, order ->
                LaunchedEffect(index) {
                    metadata.onItemRendered(index)
                }
                OrderItem(order)
            }
        }
    },
)
```

Inside the `onSuccess` lambda, `metadata`, `reload()`, and `backgroundLoadState`
are available directly via the `ContainerMapperScope` receiver, so you don't need to
prefix with `container.`.

You can also call `container.metadata.onItemRendered(index)` when you need
to reference it from outside the `fold` block.

## Next Page State

`PageState` is a sealed class representing the current state of the next page
load:

```kotlin
sealed class PageState {
    data object Idle : PageState()
    data object Pending : PageState()
    data class Error(
        val exception: Exception,
        val retry: () -> Unit,
    ) : PageState()
}
```

| State | Meaning |
|-------|---------|
| `Idle` | No next-page load is in progress |
| `Pending` | The next page is currently being loaded |
| `Error` | The next-page load failed; call `retry()` to try again |

Access the current next-page state via `container.metadata.nextPageState`.
Typically you render it in a footer item at the bottom of your list:

```kotlin
LazyColumn {
    itemsIndexed(orders) { index, order ->
        LaunchedEffect(index) {
            container.metadata.onItemRendered(index)
        }
        OrderItem(order)
    }
    item {
        when (val state = container.metadata.nextPageState) {
            PageState.Pending -> CircularProgressIndicator()
            is PageState.Error -> {
                Button(
                    onClick = { state.retry() }
                ) {
                    Text("Retry")
                }
            }
            else -> {}
        }
    }
}
```

`PageLoader` also exposes a `nextPageState: StateFlow<PageState>` property
directly on the loader object, which you can use when you prefer to manage
the loader reference yourself rather than reading it from metadata.

## Retry on Error

When a page load fails, the `PageState.Error` includes a `retry` function.
Call it to re-attempt loading the failed page:

```kotlin
is PageState.Error -> {
    Column {
        Text("Failed: ${state.exception.message}")
        Button(onClick = { state.retry() }) {
            Text("Try Again")
        }
    }
}
```

If all pages fail (i.e. there are no successfully loaded pages yet), the
subject emits `Container.Error` instead, which is handled through the normal
`fold` / `when` pattern.

## Pull-to-Refresh

Combine `container.backgroundLoadState` with `container.reload(LoadConfig.SilentLoading)`
to add pull-to-refresh without hiding the currently displayed list:

```kotlin
val container by viewModel.ordersFlow.collectAsState()

PullToRefreshBox(
    isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
    onRefresh = { container.reload(LoadConfig.SilentLoading) },
) {
    container.fold(
        onPending = { CircularProgressIndicator() },
        onError = { /* ... */ },
        onSuccess = { orders ->
            LazyColumn { /* ... */ }
        },
    )
}
```

## Flow Dependencies (Filtering)

Page loaders support the same flow dependency mechanism as regular loader
functions. Use `dependsOnFlow` or `dependsOnContainerFlow` inside the
`pageLoader` block to react to external data changes.

When the dependent flow emits a new value, the loader is automatically
re-executed from the initial key:

```kotlin
private val selectedFilter = MutableStateFlow("all")

private val ordersPageLoader = pageLoader(
    initialKey = 0,
    itemId = Order::id,
) { pageIndex ->
    val filter: String = dependsOnFlow("filter") { selectedFilter }
    val orders = ordersDataSource.fetchOrders(pageIndex, filter)
    emitPage(orders)
    if (orders.isNotEmpty()) emitNextKey(pageIndex + 1)
}

fun setFilter(filter: String) {
    selectedFilter.value = filter
    // Paging restarts from page 0 automatically
}
```

See [Flow Dependencies](subjects.md#flow-dependencies-in-loader-functions)
for details on key stability and caching.

## Updating Items in a Paged List

To update individual items in the loaded list (e.g. toggling a like), use
`subject.updateIfSuccess`. The `PageLoader` ensures the updated item appears
at the correct position across pages:

```kotlin
suspend fun toggleLike(item: Item) {
    val updated = dataSource.toggleLike(item)
    subject.updateIfSuccess { list ->
        val index = list.indexOfFirst { it.id == item.id }
        if (index == -1) return@updateIfSuccess list
        list.toMutableList().apply { set(index, updated) }
    }
}
```

## PageEmitter API

Inside the `pageLoader` block, you have access to the `PageEmitter<Key, T>`
receiver:

| Method | Description |
|--------|-------------|
| `emitPage(list: List<T>)` | Emit data for the current page. Can be called multiple times (e.g. emit local data first, then remote) |
| `emitNextKey(key: Key)` | Register the key of the next page. Call once if there is a next page, or not at all if this is the last page |

`PageEmitter` also extends `FlowComposer`, giving you access to
`dependsOnFlow` and `dependsOnContainerFlow` for reactive dependencies.

## Full Example

Here is a complete example of a paged order list with pull-to-refresh, loading
and error states at the bottom of the list:

```kotlin
// --- Data Source ---

interface OrdersDataSource {
    suspend fun fetchOrders(pageIndex: Int, pageSize: Int = 30): List<Order>
}

// --- Pager ---

@Singleton
class OrderPager @Inject constructor(
    private val ordersDataSource: OrdersDataSource,
) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader(
            initialKey = 0,
            itemId = Order::id,
        ) { pageIndex ->
            val orders = ordersDataSource.fetchOrders(pageIndex)
            emitPage(orders)
            if (orders.isNotEmpty()) emitNextKey(pageIndex + 1)
        }
    )

    fun listenOrders(): Flow<Container<List<Order>>> = subject.listenReloadable()
}

// --- ViewModel ---

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderPager: OrderPager,
) : ViewModel() {

    val ordersFlow: StateFlow<Container<List<Order>>> = orderPager.listenOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), pendingContainer())
}

// --- UI ---

@Composable
fun OrdersScreen(viewModel: OrdersViewModel = hiltViewModel()) {
    val container by viewModel.ordersFlow.collectAsState()

    PullToRefreshBox(
        isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
        onRefresh = { container.reload(LoadConfig.SilentLoading) },
    ) {
        container.fold(
            onPending = { CircularProgressIndicator() },
            onError = { exception ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to load orders: ${exception.message}")
                    Button(onClick = ::reload) {
                        Text("Try Again")
                    }
                }
            },
            onSuccess = { orders ->
                LazyColumn {
                    itemsIndexed(orders, key = { _, o -> o.id }) { index, order ->
                        LaunchedEffect(index) {
                            metadata.onItemRendered(index)
                        }
                        OrderItem(order)
                    }
                    // Next-page loading/error indicator at the bottom:
                    item {
                        when (val state = metadata.nextPageState) {
                            PageState.Pending -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                )
                            }
                            is PageState.Error -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Error: ${state.exception.message}")
                                    Button(onClick = { state.retry() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            },
        )
    }
}
```
