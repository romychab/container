# Pagination Reference

`pageLoader` builds a `ValueLoader<List<T>>` for infinite-scroll lists: pass it
as `LazyFlowSubject.create`'s `valueLoader`, and everything from
[`subjects.md`](subjects.md) (`listenReloadable`, `reload`, `updateIfSuccess`,
flow dependencies, metadata) keeps working exactly as documented there. This
file only adds what is specific to paged data - concatenating pages,
next-page state, and driving loads from the UI as items scroll into view.

See [`container-type.md`](container-type.md) for `Container<T>`, `fold`, and
the extraction functions used throughout the examples below, and
[`subjects.md`](subjects.md) for `LazyFlowSubject`, `LoadConfig`,
`ContainerMetadata`, and `dependsOnFlow` / `dependsOnContainerFlow`.

All symbols below live in package `com.elveum.container.subject.paging` unless
noted otherwise.

## 1. Overview

`pageLoader<Key, T>` produces a `PageLoader<Key, T>`, which is a
`ValueLoader<List<T>>` - so a paged list is just a `LazyFlowSubject<List<T>>`
whose loader happens to be built by `pageLoader` instead of written by hand.
Nothing about `listenReloadable()`, `reload()`, or `updateIfSuccess` changes;
the subject still holds one `Container<List<T>>`.

Inside the `pageLoader` block, one call loads *one page*. The library:

- concatenates every loaded page into a single `List<T>`, in page order;
- tracks the next-page load state (idle / loading / failed-with-retry)
  separately from the overall container state;
- triggers loading of the next page automatically as the user scrolls,
  once you wire up `onItemRendered` from the UI (section 5).

## 2. Basic usage

```kotlin
private data class Order(val id: Long, val title: String)
private data class OrderPage(val orders: List<Order>, val nextKey: Int?)

private class OrdersDataSource {
    suspend fun fetchPage(key: Int): OrderPage = TODO()
}

private class OrderRepository(private val dataSource: OrdersDataSource) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int, Order>(
            initialKey = 0,
            itemId = Order::id,
        ) { pageKey ->
            val page = dataSource.fetchPage(pageKey)
            emitPage(page.orders)
            page.nextKey?.let { emitNextKey(it) }
        }
    )

    fun listenOrders(): Flow<Container<List<Order>>> = subject.listenReloadable()
}
```

The `pageLoader` signature:

```kotlin
fun <Key, T> pageLoader(
    initialKey: Key,
    itemId: (T) -> Any,
    fetchDistance: Int = 10,
    emitMetadata: Boolean = true,
    block: suspend PageEmitter<Key, T>.(Key) -> Unit,
): PageLoader<Key, T>
```

| Parameter | Meaning |
|-----------|---------|
| `initialKey` | The key passed to `block` for the first page. Often `0`/`1` for index-based paging, or a nullable cursor (`null` for "first page") for cursor-based APIs |
| `itemId` | A function returning a stable, unique id per item (section 4) |
| `fetchDistance` | How many items before the end of the currently loaded list trigger the next page load, once `onItemRendered` reports an index that close to the end (default `10`) |
| `emitMetadata` | Whether emitted containers carry `NextPageStateMetadata` and `OnItemRenderedCallbackMetadata` (default `true`; see section 5-6) |
| `block` | Called once per page, with the page's key as its argument, running with a `PageEmitter<Key, T>` receiver (section 3) |

`PageLoader<Key, T>` itself also exposes two members directly on the loader
object, useful when you keep a reference to it instead of always going
through container metadata:

```kotlin
public interface PageLoader<Key, T> : StatefulValueLoader<List<T>> {
    public val nextPageState: StateFlow<PageState>
    public fun onItemRendered(index: Int)
}
```

## 3. `PageEmitter`

Inside the `pageLoader` block, `this` is a `PageEmitter<Key, T>`:

```kotlin
public interface PageEmitter<Key, T> : FlowComposer {
    public val metadata: ContainerMetadata
    public val loadConfig: LoadConfig
    public suspend fun emitPage(list: List<T>, metadata: ContainerMetadata = EmptyMetadata)
    public suspend fun emitNextKey(key: Key)
}
```

- **`emitPage(list, metadata = EmptyMetadata)`** - provide the items for the
  page currently loading. Call it one or more times per page (e.g. emit a
  locally-cached page first, then the remote result for the same key). The
  optional `metadata` argument is merged into the final output container's
  metadata (see section 7 for the built-in use of this with
  `TotalPagedItemsCountMetadata`).
- **`emitNextKey(key)`** - register the key for the next page. Can be called
  0, 1, or multiple times - e.g. once with a key from local storage, then
  again with a key from a remote source, mirroring the local-then-remote
  `emitPage` pattern above. Calling it zero times is how a page loader
  signals "no more data" - there is no separate "end of list" flag.
- **`metadata`** - the input metadata attached to whatever triggered this
  particular page load (a fresh scroll, a retry, a reload).
- **`loadConfig`** - the `LoadConfig` this load is running under.
- `PageEmitter` extends `FlowComposer`, so `dependsOnFlow` /
  `dependsOnContainerFlow` are available inside the block exactly as in a
  plain subject loader (section 9).

## 4. `itemId` and why it must be unique

`itemId` is how the library merges pages into one list and figures out which
positions changed when a page reloads. Two items that report the same id -
whether they are duplicated within one page, or the same logical item shows
up on more than one page (common with live, shifting backend pagination) -
are **not** kept as duplicates: the merge step deduplicates by id (first
occurrence wins), so a colliding item simply disappears from the merged
list instead of appearing twice.

Because `itemId` also becomes the natural choice for a Compose
`LazyColumn`/`LazyRow`'s `key = { }` lambda, a duplicate id passed straight
through to Compose crashes the list composable independently of anything
`pageLoader` does. Always derive `itemId` from something that is unique
across the *entire* list, not just unique within a single page (a database
primary key, not a per-page array index).

```kotlin
pageLoader<Int, Order>(
    initialKey = 0,
    itemId = Order::id,   // stable across pages and reloads
) { /* ... */ }
```

## 5. Driving next-page loads from the UI

The loader only knows to fetch the next page once it is told which items the
user has actually scrolled to. When `emitMetadata = true` (the default), every
emitted container carries an `OnItemRenderedCallbackMetadata`, exposed as the
`ContainerMetadata.onItemRendered(index: Int)` extension:

```kotlin
public fun ContainerMetadata.onItemRendered(index: Int)
```

Call it once per rendered item, keyed by that item's index, from inside a
`LazyColumn`/`LazyVerticalGrid`:

```kotlin
container.fold(
    onPending = { CircularProgressIndicator() },
    onError = { /* ... */ },
    onSuccess = { orders ->
        // `metadata` is available directly as the ContainerMapperScope receiver here
        LazyColumn {
            itemsIndexed(orders, key = { _, order -> order.id }) { index, order ->
                LaunchedEffect(index) {
                    onItemRendered(index)
                }
                OrderItem(order)
            }
        }
    },
)
```

Inside a `fold` branch `onItemRendered`, `nextPageState` and
`totalPagedItemsCount` are members of the `ContainerMapperScope` receiver, so
no prefix is needed. Outside `fold`, call them on the container -
`container.onItemRendered(index)`
instead. Once the reported index comes within `fetchDistance` items of the end
of the currently loaded list, the loader starts fetching the next page
(assuming `emitNextKey` registered one).

> The Compose-side calls in this section and section 6 (`onItemRendered`,
> `nextPageState`) require Jetpack Compose to compile and are
> verified separately, against a live paged screen, rather than in this
> file's plain-Kotlin scratch check.

## 6. Next-page state

`PageState` is a sealed class describing the next page's load state,
independent of the overall `Container<List<T>>` state:

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
| `Idle` | No next-page load in progress (includes "no next page exists") |
| `Pending` | The next page is being fetched right now |
| `Error` | The next-page load failed; call `retry()` to retry that same page |

Read it through the `ContainerMetadata.nextPageState` extension:

```kotlin
public val ContainerMetadata.nextPageState: PageState
```

Typically rendered as a footer item at the end of the list:

```kotlin
LazyColumn {
    itemsIndexed(orders, key = { _, order -> order.id }) { index, order ->
        LaunchedEffect(index) { onItemRendered(index) }
        OrderItem(order)
    }
    item {
        when (val state = nextPageState) {
            PageState.Idle -> {}
            PageState.Pending -> CircularProgressIndicator()
            is PageState.Error -> Button(onClick = { state.retry() }) {
                Text("Retry: ${state.exception.message}")
            }
        }
    }
}
```

If **every** page fails - i.e. there is no successfully loaded page yet - the
whole subject emits `Container.Error` instead of a `PageState.Error`; handle
that the normal way through `fold`/`when` on the container itself. A
`PageState.Error` only ever describes a *next*-page failure on top of an
already-visible list.

## 7. Total item count

A data source that reports a total count up front can attach it via the
optional `metadata` argument of `emitPage`, using `TotalPagedItemsCountMetadata`:

```kotlin
public data class TotalPagedItemsCountMetadata(
    val totalPagedItemsCount: Int,
) : ContainerMetadata
```

```kotlin
emitPage(page.orders, TotalPagedItemsCountMetadata(totalPagedItemsCount = response.total))
```

Read it back with the `ContainerMetadata.totalPagedItemsCount` extension:

```kotlin
public val ContainerMetadata.totalPagedItemsCount: Int
```

```kotlin
val total: Int = container.totalPagedItemsCount
```

It returns **`-1`** when no page has attached `TotalPagedItemsCountMetadata` -
treat `-1` as "unknown", not as a real count of zero.

## 8. Pull-to-refresh on a paged list

A paged subject supports pull-to-refresh the same way any other
`LazyFlowSubject` does (see `subjects.md`'s `LoadConfig` section): reload with
`LoadConfig.SilentLoading` so the currently loaded pages stay on screen while
page 1 reloads from `initialKey`, and drive the refresh indicator off
`backgroundLoadState`. The config applies to that reload only:

```kotlin
val container by viewModel.ordersFlow.collectAsState()

PullToRefreshBox(
    isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
    onRefresh = {
        container.reload(LoadConfig.SilentLoading)
    },
) {
    container.fold(
        onPending = { CircularProgressIndicator() },
        onError = { /* ... */ },
        onSuccess = { orders -> /* LazyColumn as above */ },
    )
}
```

A silent reload restarts paging from `initialKey`, discarding the previously
loaded pages once the new page 1 arrives - it is a full refresh of the list,
not an in-place patch.

## 9. Flow dependencies for filtering

`PageEmitter` extends `FlowComposer`, so `dependsOnFlow` /
`dependsOnContainerFlow` work inside the `pageLoader` block exactly as
described in `subjects.md`'s flow-dependencies section. When the dependency
emits a new value, the whole paged loader restarts from `initialKey` -
exactly like a category filter, search query, or sort order changing:

```kotlin
private val selectedFilter = MutableStateFlow("all")

private val ordersPageLoader = pageLoader<Int, Order>(
    initialKey = 0,
    itemId = Order::id,
) { pageKey ->
    val config = FlowComposer.Config(LoadConfig.SilentLoading)
    val filter: String = dependsOnFlow("filter", config) { selectedFilter }
    val page = dataSource.fetchPage(pageKey)
    emitPage(page.orders.filter { filter == "all" || it.title.contains(filter) })
    page.nextKey?.let { emitNextKey(it) }
}
```

By default a dependency-triggered restart resets the list straight to
`Container.Pending`, same as any other loader. Passing a `FlowComposer.Config`
as an extra key (as above) keeps the currently loaded pages visible while the
new filter's first page loads - the same mechanism `subjects.md` documents
for non-paged loaders.

## 10. Updating individual items in a paged list

Because the subject underneath is a plain `LazyFlowSubject<List<T>>`,
`updateIfSuccess` (from `subjects.md`) works directly against the merged
list - no paging-specific update API is needed:

```kotlin
suspend fun toggleLike(item: Order) {
    val updated = dataSource.toggleLike(item)
    subject.updateIfSuccess { list ->
        list.map { if (it.id == updated.id) updated else it }
    }
}
```

The update finds and replaces the item wherever it currently sits in the
merged list, regardless of which page it originally came from - pagination
state (`nextPageState`, loaded pages) is untouched by an `updateIfSuccess`
call.

## 11. Full example

A full example: a repository, a `ViewModel`, and a screen combining
pull-to-refresh, paging, and next-page error handling with retry.

```kotlin
// --- Data source ---

interface BookDataSource {
    suspend fun fetchPage(key: Int?): BookPage
}

data class Book(val id: Int, val title: String, val author: String)
data class BookPage(val books: List<Book>, val nextPageKey: Int?)

// --- Repository ---

class BookRepository @Inject constructor(
    private val dataSource: BookDataSource,
) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int?, Book>(
            initialKey = null,
            itemId = Book::id,
        ) { pageKey ->
            val result = dataSource.fetchPage(pageKey)
            emitPage(result.books)
            if (result.nextPageKey != null) emitNextKey(result.nextPageKey)
        }
    )

    fun getBooks(): Flow<Container<List<Book>>> = subject.listenReloadable()
}

// --- ViewModel ---

@HiltViewModel
class BooksViewModel @Inject constructor(
    bookRepository: BookRepository,
) : ViewModel() {

    val booksFlow: StateFlow<Container<List<Book>>> = bookRepository
        .getBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), pendingContainer())
}

// --- UI ---

@Composable
fun BooksScreen(viewModel: BooksViewModel = hiltViewModel()) {
    val container by viewModel.booksFlow.collectAsState()

    PullToRefreshBox(
        isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
        onRefresh = {
            container.reload(LoadConfig.SilentLoading)
        },
    ) {
        container.fold(
            onPending = { CircularProgressIndicator() },
            onError = { exception ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to load books: ${exception.message}")
                    Button(onClick = ::reload) { Text("Try Again") }
                }
            },
            onSuccess = { books ->
                LazyColumn {
                    itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
                        LaunchedEffect(index) {
                            onItemRendered(index)
                        }
                        BookCard(book)
                    }
                    item {
                        when (val state = nextPageState) {
                            PageState.Idle -> {}
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
                                    Button(onClick = { state.retry() }) { Text("Retry") }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}
```
