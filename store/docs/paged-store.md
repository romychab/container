# Paged Store

`PagedStore<T>` loads data in chunks (pages) and merges them into a single
`List<T>` that can be rendered in lazy containers with endless scrolling.
Typical data managed by a paged store: news feed, posts, gallery, search
results.

## Table of Contents

- [Creating a Paged Store](#creating-a-paged-store)
- [PagedList](#pagedlist)
  - [Total Item Count](#total-item-count)
- [Triggering Next-Page Loads](#triggering-next-page-loads)
- [Next-Page State](#next-page-state)
- [Reloading and Pull-to-Refresh](#reloading-and-pull-to-refresh)
- [Queries](#queries)
- [Local Storage](#local-storage)
- [Updating Items](#updating-items)
- [Full Example](#full-example)
- [API Summary](#api-summary)

## Creating a Paged Store

`pagedStoreBuilder` requires two arguments:

- `initialKey` - the key of the first page (page index, cursor string,
  limit/offset object, etc.)
- `itemId` - a function returning a unique ID per item, used to
  deduplicate items that appear in more than one page

The `onFetch` function receives a page key and returns a
[`PagedList`](#pagedlist):

```kotlin
class PhotoRepository(
    private val dataSource: PhotoDataSource,
) {

    private val store = StoreFactory.pagedStoreBuilder<Int, Photo>(
        initialKey = 0,
        itemId = Photo::id,
    ).build(
        onFetch = { pageKey ->
            val page = dataSource.fetchPage(pageKey)
            PagedList(items = page.photos, nextKey = page.nextKey)
        }
    )

    fun getPhotos(): Flow<StoreResult<List<Photo>>> = store.observe()
}
```

In addition to the base `setInMemoryCacheTimeout` / `setCoroutineContext`
options, paged builders provide `setFetchDistance`:

```kotlin
private val store = StoreFactory.pagedStoreBuilder<Int, Photo>(0, Photo::id)
    .setFetchDistance(10) // default: 20
    .build(onFetch = dataSource::fetchPage)
```

The fetch distance defines how close to the end of the loaded list the
user must scroll before the next page starts loading.

## PagedList

`PagedList<PageKey, T>` represents one fetched chunk of data:

```kotlin
public data class PagedList<PageKey : Any, T : Any>(
    val items: List<T>,
    val nextKey: PageKey?,
    val metadata: ContainerMetadata = EmptyMetadata,
)
```

- `items` - the items of the page
- `nextKey` - the key of the next page, or `null` when there are no more
  pages
- `metadata` - optional metadata attached to the page; it is merged into the
  metadata of the emitted `StoreResult` (see [Total Item Count](#total-item-count))

The store concatenates the `items` of all loaded pages and emits them as a
single `List<T>` inside `StoreResult.Loaded`.

### Total Item Count

Many paginated APIs return the total number of items along with each page.
Pass it via the `totalCount` constructor of `PagedList`:

```kotlin
onFetch = { pageKey ->
    val page = dataSource.fetchPage(pageKey)
    PagedList(items = page.photos, nextKey = page.nextKey, totalCount = page.total)
}
```

This is a shortcut for attaching `TotalPagedItemsCountMetadata` to the page.
Read it back from the emitted result via the `totalPagedItemsCount` extension
property (it returns `-1` when no total count was provided):

```kotlin
val total: Int = result.metadata.totalPagedItemsCount
```

When pages report different totals, the value from the **most recently loaded
page** wins. More generally, any `ContainerMetadata` attached to a page is
propagated to the merged result, so you can surface arbitrary per-page
information (see [Container metadata](../../README.md)).

## Triggering Next-Page Loads

The store does not know which items are currently visible, so the UI must
report rendered items via `onItemRendered(index)`. When the rendered index
gets within the fetch distance of the end of the loaded list, the store
loads the next page automatically:

```kotlin
LazyColumn {
    itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
        LaunchedEffect(index) {
            viewModel.onItemRendered(index) // forwards to store.onItemRendered(index)
        }
        PhotoCard(photo)
    }
}
```

Pages are appended to the existing list, and observers receive the updated
merged list with every loaded page.

## Next-Page State

While the merged list is exposed through the regular `StoreResult`, the
status of the *next page* load is published via metadata. Use the
`nextPageState` extension property to read it:

| Value               | Meaning                                                   |
|---------------------|-----------------------------------------------------------|
| `PageState.Idle`    | No next-page load in progress (or no more pages)          |
| `PageState.Pending` | The next page is being loaded                             |
| `PageState.Error`   | The next page failed to load; call `retry()` to try again |

Render it as a list footer:

```kotlin
LazyColumn {
    itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
        LaunchedEffect(index) { viewModel.onItemRendered(index) }
        BookCard(book)
    }
    item {
        when (val pageState = result.nextPageState) {
            PageState.Idle -> Unit
            PageState.Pending -> CircularProgressIndicator()
            is PageState.Error -> OutlinedButton(onClick = pageState.retry) {
                Text("Retry")
            }
        }
    }
}
```

Note the difference between the two error channels:

- a failed **initial** load emits `StoreResult.Failed` - render a
  full-screen error
- a failed **next-page** load keeps the already loaded items in
  `StoreResult.Loaded` and reports the failure via
  `nextPageState` - render a footer with a retry button

## Reloading and Pull-to-Refresh

`invalidate` / `invalidateAsync` reset the pagination and reload from the
initial key:

```kotlin
// "try again" after a failed initial load - show the Loading state
fun tryAgain() = store.invalidateAsync()

// pull-to-refresh - keep current items visible while reloading
fun refresh() = store.invalidateAsync(LoadRequest.Silent)
```

With `LoadRequest.Silent`, the refresh progress is observable via
`result.isBackgroundLoading()`:

```kotlin
PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = viewModel::refresh,
) { /* LazyColumn */ }
```

## Queries

`withQuery` turns a `PagedStore<T>` into a `PagedQueryStore<Q, T>`. The
`onFetch` function receives both the query and the page key, and
submitting a new query **resets the pagination** and re-fetches from the
initial key:

```kotlin
private val store = StoreFactory.pagedStoreBuilder<Int, Photo>(0, Photo::id)
    .withQuery<Set<PhotoCategory>>(initialQuery = PhotoCategory.entries.toSet())
    .build(
        onFetch = { categories, pageKey -> dataSource.fetchPage(categories, pageKey) }
    )

fun getSelectedCategories(): StateFlow<Set<PhotoCategory>> = store.queryFlow

fun toggleCategory(category: PhotoCategory) {
    val current = store.queryFlow.value
    val updated = if (category in current) current - category else current + category
    store.submitQueryAsync(updated)
}
```

As with simple stores, `withQuery` accepts an optional `debounceMillis`
parameter for search-as-you-type scenarios, and `submitQuery` uses
`LoadRequest.Silent` by default.

## Local Storage

Paged stores support the same local storage modes as other stores. The
storage callbacks operate **per page** - they receive the page key and a
`PagedList`:

```kotlin
private val store = StoreFactory.pagedStoreBuilder<Int, Article>(0, Article::id)
    .addSuspendingLocalStorage()
    .build(
        onFetch = remoteArticlesSource::fetchArticles,
        onSaveToStorage = { pageKey, pagedList ->
            localArticlesSource.saveLocalArticles(pageKey, pagedList)
        },
        onLoadFromStorage = { pageKey ->
            localArticlesSource.fetchLocalArticles(pageKey) // PagedList<Int, Article>?
        },
    )
```

Each page is first read from the local storage (and shown immediately if
present), then refreshed from the remote source and saved back.

When a query is configured, the storage callbacks additionally receive the
query:

```kotlin
private val store = StoreFactory.pagedStoreBuilder<Int, Item>(0, Item::id)
    .withQuery(initialQuery = "")
    .addSuspendingLocalStorage()
    .build(
        onFetch = { query, pageKey -> api.fetchPage(query, pageKey) },
        onSaveToStorage = { query, pageKey, pagedList -> dao.save(query, pageKey, pagedList) },
        onLoadFromStorage = { query, pageKey -> dao.load(query, pageKey) },
    )
```

Contract-based `build` overloads are available as well: `PagedContract`,
`PagedSuspendingContract` and their `Query` variants.

## Updating Items

`optimisticUpdate` operates on the whole merged list, so per-item updates
work the same way as in simple stores - replace the item and emit the new
list:

```kotlin
suspend fun toggleLike(product: Product) {
    store.optimisticUpdate { oldList ->
        val index = oldList.indexOfFirst { it.id == product.id }
        if (index != -1) {
            val newList = oldList.toMutableList().apply {
                set(index, product.copy(isLiked = !product.isLiked))
            }
            emit(newList) // shown immediately; reverted if toggleLike fails
        }
        dataSource.toggleLike(product)
    }
}
```

`get()` returns the latest merged list result synchronously, and
`updateWith` replaces the cached result entirely (including `Loading` /
`Failed` states):

```kotlin
val current: StoreResult<List<Product>> = store.get()
store.updateWith(StoreResult.Loaded(products))
```

## Full Example

A repository and screen combining pagination, pull-to-refresh and
next-page error handling:

```kotlin
class BookRepository(
    private val dataSource: BookDataSource,
) {

    private val store = StoreFactory.pagedStoreBuilder<Int, Book>(
        initialKey = 0,
        itemId = Book::id,
    ).build(onFetch = dataSource::fetchPage)

    fun getBooks(): Flow<StoreResult<List<Book>>> = store.observe()
    fun refresh() = store.invalidateAsync(LoadRequest.Silent)
    fun tryAgain() = store.invalidateAsync()
    fun onItemRendered(index: Int) = store.onItemRendered(index)
}
```

```kotlin
when (val result = state) {
    StoreResult.Loading -> CircularProgressIndicator()
    is StoreResult.Failed -> ErrorScreen(onTryAgain = viewModel::tryAgain)
    is StoreResult.Loaded -> {
        PullToRefreshBox(
            isRefreshing = result.isBackgroundLoading(),
            onRefresh = viewModel::refresh,
        ) {
            LazyColumn {
                itemsIndexed(result.value, key = { _, book -> book.id }) { index, book ->
                    LaunchedEffect(index) { viewModel.onItemRendered(index) }
                    BookCard(book)
                }
                item { NextPageFooter(pageState = result.nextPageState) }
            }
        }
    }
}
```

## API Summary

| Member                                             | Description                                              |
|----------------------------------------------------|----------------------------------------------------------|
| `observe(request)`                                 | Observe the merged list as `Flow<StoreResult<List<T>>>`  |
| `get()`                                            | Read the latest merged-list `StoreResult` synchronously  |
| `onItemRendered(index)`                            | Report a rendered item; may trigger the next-page load   |
| `invalidate(request)` / `invalidateAsync(request)` | Reset pagination and reload                              |
| `optimisticUpdate { }` / `update { }`              | Update the merged list in the cache                      |
| `updateWith(storeResult)`                          | Replace the cached result with any `StoreResult`         |
| `whenActive { }`                                   | Run a block while the store has observers                |
| `queryFlow`, `submitQuery`, `submitQueryAsync`     | Query support (`PagedQueryStore` only)                   |
| `result.nextPageState`                             | `Idle` / `Pending` / `Error` state of the next-page load |
| `result.metadata.totalPagedItemsCount`             | Total item count reported via `PagedList(totalCount = …)` |
