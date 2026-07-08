# Store

[![Maven Central](https://img.shields.io/maven-central/v/com.elveum/store.svg?label=Maven%20Central&color=dark-green)](https://uandcode.com/sh/store)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)
[![License: Apache 2](https://img.shields.io/github/license/romychab/container)](../LICENSE)
[![PR Check](https://github.com/romychab/container/actions/workflows/pr-check.yml/badge.svg)](https://github.com/romychab/container/actions/workflows/pr-check.yml)
[![Coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/romychab/90ef83eacc2f4ce17e4c53c8bb255295/raw/store-coverage.json)](https://github.com/romychab/container/actions/workflows/publish.yml)
[![Publish](https://github.com/romychab/container/actions/workflows/publish.yml/badge.svg)](https://github.com/romychab/container/actions/workflows/publish.yml)

Store is a library for caching and observing data in Android applications.
It wraps your data sources (network, database, in-memory) into a single
**store** object that loads data on demand, caches it in memory, keeps it
in sync with a local storage, and exposes it as a reactive
`Flow<StoreResult<T>>` to any number of observers.

The library is built on top of the [Container](../README.md) library and
re-uses its `Container` metadata, pagination and reducer machinery under
the hood.

## Table of Contents

- [Installation](#installation)
- [Core Concepts](#core-concepts)
  - [StoreFactory](#storefactory)
  - [StoreResult](#storeresult)
  - [Simple Store](#simple-store)
  - [Keyed Store](#keyed-store)
  - [Paged Store](#paged-store)
- [Adding Local Storage](#adding-local-storage)
- [Queries](#queries)
- [Updating Cached Data](#updating-cached-data)
- [Load Requests](#load-requests)
- [Consuming Stores in ViewModels](#consuming-stores-in-viewmodels)
- [Detailed Documentation](#detailed-documentation)

## Installation

Add the following line to your `build.gradle` file:

```
implementation "com.elveum:store:3.3.1"
```

The `store` artifact depends on `com.elveum:container`, so the Container
library is added to your project automatically.

## Core Concepts

The library provides three types of stores, all created via `StoreFactory`:

- **`SimpleStore<T>`** - fetches and caches a single value without keys
  (user profile, settings, shopping cart, non-paged lists)
- **`KeyedStore<Key, T>`** - acts like a map of stores; each key has its own
  cached value with an auto-managed lifecycle (product details, friend
  profiles, movie details)
- **`PagedStore<T>`** - loads data in chunks/pages and merges them into a
  single list for endless scrolling (news feed, gallery, posts)

Every store:

- starts loading lazily, when the first observer subscribes
- shares one in-memory cache between all observers
- releases the cache after a configurable timeout (5 seconds by default)
  once the last observer unsubscribes
- emits `StoreResult.Loading`, `StoreResult.Loaded` or `StoreResult.Failed`
  to its observers

### StoreFactory

`StoreFactory` provides three builders, one per store type. The companion
object is a default implementation, so you can create stores without any
setup:

```kotlin
val store = StoreFactory.simpleStoreBuilder<UserProfile>()
    .build(onFetch = { api.fetchUserProfile() })
```

For better testability, inject `StoreFactory` as a dependency:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object StoreFactoryModule {
    @Provides
    fun provideStoreFactory(): StoreFactory = StoreFactory
}

@Singleton
class MyRepository @Inject constructor(
    private val storeFactory: StoreFactory,
)
```

All builders share a few base options:

```kotlin
val store = factory.simpleStoreBuilder<UserProfile>()
    .setInMemoryCacheTimeout(60.seconds) // default is 5 seconds
    .setCoroutineContext(Dispatchers.IO) // context for fetch/storage calls
    .setLoadRequest(LoadRequest.Silent)  // default request used by observe/invalidate/invalidateAsync
    .build(onFetch = { api.fetchUserProfile() })
```

### StoreResult

`StoreResult<T>` is a sealed class emitted by all stores. It has three
states:

- `StoreResult.Loading` - no data in the in-memory cache yet, the store is
  loading it from the configured data sources
- `StoreResult.Loaded<T>` - data has been loaded successfully and holds a
  `value: T`
- `StoreResult.Failed` - the load failed and holds an `exception: Exception`

`Loaded` and `Failed` share the common supertype `StoreResult.Completed<T>`.

```kotlin
when (val result = storeResult) {
    StoreResult.Loading -> showProgressBar()
    is StoreResult.Failed -> showError(result.exception)
    is StoreResult.Loaded -> showData(result.value)
}
```

Every result also carries `metadata` with the `sourceType` (local/remote),
the `backgroundLoadState` (whether a refresh is running behind the cached
data) and, for paged stores, the `nextPageState`. See
[Store Results](docs/store-results.md) for value extraction, transformations
and combining multiple stores.

### Simple Store

`SimpleStore<T>` caches a single value. The minimal setup needs only an
`onFetch` function:

```kotlin
class UserProfileRepository(
    private val dataSource: UserProfileDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<UserProfile>()
        .build(onFetch = dataSource::fetchUserProfile)

    fun getUserProfile(): Flow<StoreResult<UserProfile>> = store.observe()

    fun reload() = store.invalidateAsync()
}
```

The fetch runs when the first observer subscribes; later observers get the
cached value instantly. See [Simple Store](docs/simple-store.md) for the
full guide.

### Keyed Store

`KeyedStore<Key, T>` manages multiple values, one per key. Each key-value
entry lives while at least one observer listens to it (plus the in-memory
cache timeout):

```kotlin
class ProductDetailsRepository(
    private val dataSource: ProductsDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<ProductDetails>().withKeys<Long>()
        .setInMemoryCacheTimeout(10.seconds)
        .build(onFetch = dataSource::fetchProductById)

    fun getProductById(id: Long): Flow<StoreResult<ProductDetails>> {
        return store.observe(id)
    }
}
```

This is a natural fit for master-detail screens: the details screen and any
other screen observing the same key share one cached value, and an
`optimisticUpdate(key)` is instantly visible everywhere. See
[Keyed Store](docs/keyed-store.md).

### Paged Store

`PagedStore<T>` loads data page by page and merges all loaded pages into a
single `List<T>`. The `onFetch` function receives a page key and returns a
`PagedList` with the items and the next page key (or `null` if there are no
more pages):

```kotlin
class PhotoRepository(
    private val dataSource: PhotoDataSource,
) {

    private val store = StoreFactory.pagedStoreBuilder<Int, Photo>(
        initialKey = 0,
        itemId = Photo::id, // used for deduplication of items
    ).build(
        onFetch = { pageKey ->
            val page = dataSource.fetchPage(pageKey)
            PagedList(items = page.photos, nextKey = page.nextKey)
        }
    )

    fun getPhotos(): Flow<StoreResult<List<Photo>>> = store.observe()

    fun onItemRendered(index: Int) = store.onItemRendered(index)
}
```

Call `onItemRendered(index)` from your UI as list items become visible -
the store automatically loads the next page when the rendered item is close
enough to the end of the list (the *fetch distance*, 20 items by default).
See [Paged Store](docs/paged-store.md).

## Adding Local Storage

Every builder can attach a local storage layer (e.g. Room DAO, DataStore)
in addition to the remote `onFetch`. Cached data is then loaded from the
local storage first and shown immediately, while a fresh value is fetched
from the remote source in the background.

For storages exposed via suspend functions, use
`addSuspendingLocalStorage()`:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<List<Article>>()
    .addSuspendingLocalStorage()
    .build(
        onFetch = remoteSource::fetchArticles,
        onSaveToStorage = localSource::saveArticles,
        onLoadFromStorage = localSource::loadArticles, // returns T? (null = no data)
    )
```

For storages exposed via `Flow` (Room `@Query` returning `Flow`, DataStore),
use `addReactiveLocalStorage()` - any change in the local storage is then
automatically pushed to all store observers:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<List<Article>>()
    .addReactiveLocalStorage()
    .build(
        onFetch = remoteSource::fetchArticles,
        onSaveToStorage = localSource::saveArticles,
        onObserveStorage = localSource::observeArticles, // returns Flow<T?>
    )
```

Suspending local storage is available for simple, keyed and paged stores
(keyed/paged variants additionally receive the key in each callback).
Reactive local storage is supported by simple and keyed stores; for paged
stores it is planned for future versions. Instead of separate lambdas, you
can also pass a single *contract* interface implementation - see the
per-store docs for details.

When the data lives **only** in a local reactive source and there is no
remote `onFetch` at all, call `disableFetcher()` and provide just an
`onObserve` lambda returning the local `Flow`:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<Settings>()
    .disableFetcher()
    .build(onObserve = settingsDataStore::observeSettings) // () -> Flow<Settings>
```

This is supported by simple and keyed stores (and combines with
`withQuery`). See [Simple Store](docs/simple-store.md#local-only-stores-no-fetcher)
and [Keyed Store](docs/keyed-store.md#local-only-stores-no-fetcher).

## Queries

Simple and paged stores can be parameterized with a query (search text,
filter, etc.) via `withQuery`. Submitting a new query re-fetches the data;
for paged stores it also resets the pagination:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<List<GalleryImage>>()
    .withQuery(initialQuery = "", debounceMillis = 500)
    .build(onFetch = { query -> remoteSource.fetchImages(query) })

fun setQuery(query: String) = store.submitQueryAsync(query)

val currentQuery: StateFlow<String> get() = store.queryFlow
```

### External query flow

When the query already lives elsewhere as a reactive stream (a search
`StateFlow`, a combination of filters), pass it to `withQuery` as a lambda
returning a `Flow` instead of calling `submitQuery` yourself. The
store follows that flow and is a plain store (`SimpleStore` / `KeyedStore` /
`PagedStore`) with **no** `submitQuery`/`queryFlow` surface:

```kotlin
private val searchQuery = MutableStateFlow<String>("")
private val store = StoreFactory.simpleStoreBuilder<List<GalleryImage>>()
    .withQuery(debounceMillis = 500) { searchQuery } // searchQuery: StateFlow<String>
    .build { query -> remoteSource.fetchImages(query) } // -> SimpleStore<List<GalleryImage>>

fun getImages(): Flow<StoreResult<List<GalleryImage>>> = store.observe()
fun setSearchQuery(query: String) = searchQuery.update { query }
```

A `StateFlow` needs no initial query (it is derived from `.value`). For a plain
`Flow`, supply an `initialQuery` for the immediate first load:
`withQuery(initialQuery = "") { queryFlow }`. Keyed stores receive the key, so
each key can follow its own query flow:
`withQuery { key -> queryFlowFor(key) }`. For paged stores a new query resets
pagination and reloads from the first page. This overload is available on every
builder that supports `withQuery`.

## Updating Cached Data

All stores support optimistic updates: emit the expected new value to the
in-memory cache first (so the UI updates instantly), then perform the real
update. If the real update throws, the emitted value is automatically
reverted:

```kotlin
suspend fun toggleLike(image: GalleryImage) {
    store.optimisticUpdate { oldList ->
        val updated = image.copy(isLiked = !image.isLiked)
        emit(oldList.map { if (it.id == updated.id) updated else it })
        remoteSource.updateImage(updated) // reverted automatically on failure
    }
}
```

To transform the currently loaded value after the real data source has
already been changed, use `updateIfSuccess` (a read-modify-write helper that
runs only when the current value is `Loaded`):

```kotlin
suspend fun renameProfile(newName: String) {
    dataSource.renameProfile(newName)
    store.updateIfSuccess { it.copy(name = newName) }
}
```

To replace the cached result regardless of the current state, use `updateWith`:

```kotlin
suspend fun clearCart() {
    cartDataSource.clear()
    store.updateWith(StoreResult.Loaded(emptyList()))
}
```

Note that `optimisticUpdate`, `updateIfSuccess`, `submitQuery` and
`submitQueryAsync` act on the in-memory cache, which exists only while the
store (or key) has at least one active observer; calling them for a store that
is not currently observed is a no-op.

## Load Requests

A `LoadRequest` controls which sources are used and how the loading state
is shown. It is accepted in exactly two places: on `observe`, and on the
store builder via `setLoadRequest(...)`. The mutating/query calls
(`invalidate`, `invalidateAsync`, `submitQuery`, `submitQueryAsync`) do
**not** take a request.

`observe` accepts a *nullable* `LoadRequest`; omitting it (or passing
`null`) falls back to the store's configured default request, which is
`LoadRequest.Default` unless overridden via `setLoadRequest(...)` on the
builder:

```kotlin
// Use the store's configured default request
fun getUserProfile() = store.observe()

// Keep current content visible while reloading (per this observer)
fun getUserProfileSilently() = store.observe(LoadRequest.Silent)

// Custom: fine-grained control
val request = LoadRequest.builder()
    .keepContentOnLoadAndError() // keep old data even if the reload fails
    .build()
fun getUserProfileKeepingContent() = store.observe(request)
```

The default request is configured once on the builder. Besides a fixed
request, `setLoadRequest` also accepts a `Flow<LoadRequest>` whose latest
emission becomes the current default - handy for reacting to changing
conditions: switching to offline mode when connectivity drops, or honoring a
user-controlled "offline mode" flag stored in DataStore/preferences and toggled
from a settings screen (a "data saver" or battery-saver setting works the same
way):

```kotlin
// Fixed default request
factory.simpleStoreBuilder<UserProfile>()
    .setLoadRequest(LoadRequest.Silent)

// Reactive default request driven by connectivity
factory.simpleStoreBuilder<UserProfile>()
    .setLoadRequest(connectivity.map { online ->
        if (online) LoadRequest.Default
        else LoadRequest.builder().offlineMode().build()
    })

// Reactive default request driven by a persisted user setting (Flow<Boolean>)
factory.simpleStoreBuilder<UserProfile>()
    .setLoadRequest(settings.offlineModeEnabled.map { offline ->
        if (offline) LoadRequest.builder().offlineMode().build()
        else LoadRequest.Default
    })
```

Invalidation intentionally does **not** take a request: a store (or a key)
may have several observers, each subscribed with its own request (fresh,
offline, keep-content, ...). `invalidate`/`invalidateAsync` simply trigger a
reload, and every observer keeps receiving data according to the request
**it** subscribed with via `observe(...)` (or the builder default).

The `LoadRequest.builder()` also supports `freshMode()` (skip caches, force
a remote fetch) and `offlineMode()` (use only cached data). See
[Load Requests](docs/load-requests.md).

## Consuming Stores in ViewModels

The most direct way is `stateIn` with `StoreResult.Loading` as the initial
value. When the UI state is richer than the raw store value, convert the
store flow into a `StoreResultReducer` - it maps loaded values into your
state class and lets you apply local updates on top:

```kotlin
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
) : ViewModel()/*, ReducerOwner */ { // ReducerOwner is optional, but you can make an abstract ViewModel
                                     // implementing this interface and then omit SharingStarted and
                                     // CoroutineScope arguments when creating StateFlow / Reducers.

    data class State(
        val cart: List<CartProductItem>,
        val isClearInProgress: Boolean = false,
    )

    private val reducer: StoreResultReducer<State> = cartRepository
        .getFullCart()
        .storeResultToReducer(
            initialState = ::State,
            nextState = State::copy,
            scope = viewModelScope,
            started = SharingStarted.Lazily,
        )

    val stateFlow: StateFlow<StoreResult<State>> = reducer.stateFlow

    fun clear() = viewModelScope.launch {
        try {
            reducer.updateState { it.copy(isClearInProgress = true) }
            cartRepository.clear()
        } finally {
            reducer.updateState { it.copy(isClearInProgress = false) }
        }
    }
}
```

`updateState` modifies only the `Loaded` state and is a no-op while the
store is loading or failed. See
[Store Results](docs/store-results.md#storeresultreducer) for the reducer
API and for combining multiple stores with `combineStores`.

## Detailed Documentation

| Topic                                          | Description                                                                                  |
|------------------------------------------------|----------------------------------------------------------------------------------------------|
| [Simple Store](docs/simple-store.md)           | `SimpleStore`, local storage, queries, optimistic updates, `whenActive`                      |
| [Keyed Store](docs/keyed-store.md)             | `KeyedStore`, per-key cache lifecycle, master-detail patterns, cache synchronization         |
| [Paged Store](docs/paged-store.md)             | `PagedStore`, `PagedList`, next-page states, queries, local storage, per-item updates        |
| [Store Results](docs/store-results.md)         | `StoreResult` states, extraction, transformations, `combineStores`, `StoreResultReducer`     |
| [Load Requests](docs/load-requests.md)         | `LoadRequest`, fresh/offline modes, keeping content on load/error, background load state     |
| [LLM Agent Skill](../skills/container-store/)  | Installable Agent Skill for AI coding agents: setup, imports reference, architecture rules   |