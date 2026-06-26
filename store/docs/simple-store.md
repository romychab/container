# Simple Store

`SimpleStore<T>` fetches and caches a single value without keys. Typical
data managed by a simple store: user profile, settings, dashboard,
shopping cart, non-paged lists.

## Table of Contents

- [Creating a Simple Store](#creating-a-simple-store)
- [Cache Lifecycle](#cache-lifecycle)
- [Observing Data](#observing-data)
- [Reloading Data](#reloading-data)
- [Local Storage](#local-storage)
  - [Suspending Local Storage](#suspending-local-storage)
  - [Reactive Local Storage](#reactive-local-storage)
  - [Contracts](#contracts)
  - [Local-Only Stores (No Fetcher)](#local-only-stores-no-fetcher)
- [Queries](#queries)
- [Updating Cached Data](#updating-cached-data)
- [Reacting to Store Activity](#reacting-to-store-activity)
- [API Summary](#api-summary)

## Creating a Simple Store

The minimal setup requires only an `onFetch` function:

```kotlin
class UserProfileRepository(
    private val dataSource: UserProfileDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<UserProfile>()
        .build(onFetch = dataSource::fetchUserProfile)

    fun getUserProfile(): Flow<StoreResult<UserProfile>> = store.observe()
}
```

The builder supports two base options before `build`:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<UserProfile>()
    .setInMemoryCacheTimeout(60.seconds)  // default: 5 seconds
    .setCoroutineContext(Dispatchers.IO)  // context used by fetch/storage calls
    .build(onFetch = dataSource::fetchUserProfile)
```

## Cache Lifecycle

A simple store is lazy and reference-counted:

- The `onFetch` function runs when the first observer starts collecting
  the flow returned by `observe()`.
- All observers share the same in-memory cached value; new observers
  receive it instantly without re-triggering the fetch.
- When the last observer unsubscribes, the cache is kept for the
  configured `setInMemoryCacheTimeout` duration (**5 seconds** by default).
  If a new observer appears within that window, the cached value is reused;
  otherwise the cache is released and the next observer triggers a fresh
  load.

## Observing Data

`observe()` returns a `Flow<StoreResult<T>>` that emits `Loading`,
`Loaded` and `Failed` results:

```kotlin
repository.getUserProfile().collect { result ->
    when (result) {
        StoreResult.Loading -> showProgressBar()
        is StoreResult.Failed -> showError(result.exception)
        is StoreResult.Loaded -> showProfile(result.value)
    }
}
```

Optionally, `observe()` accepts a [LoadRequest](load-requests.md) which
controls how data is loaded for this observation:

```kotlin
// do not show Loading if a cached value exists; refresh in background
fun getUserProfile() = store.observe(LoadRequest.Silent)
```

If you need the latest result synchronously (without collecting the flow),
use `get()`, which returns the most recent `StoreResult<T>` from the
in-memory cache:

```kotlin
val current: StoreResult<UserProfile> = store.get()
```

## Reloading Data

`invalidate` forces the store to reload its data:

```kotlin
// suspend version - waits until the reload completes (or fails)
suspend fun refresh() = store.invalidate()

// fire-and-forget version
fun reload() = store.invalidateAsync()
```

Both accept a [LoadRequest](load-requests.md). The most common pattern is
pull-to-refresh, where the old content should stay visible while
reloading:

```kotlin
fun refresh() {
    store.invalidateAsync(LoadRequest.Silent)
}
```

In the UI, the in-progress refresh can be detected via
`result.isBackgroundLoading()`:

```kotlin
PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = viewModel::refresh,
) { /* list content */ }
```

## Local Storage

By default, a simple store keeps data only in memory. You can attach a
persistent local storage (Room, DataStore, files) so that:

1. When a load starts, the store first tries to read the value from the
   local storage and emits it immediately (if present).
2. The remote `onFetch` then runs in the background; its result is saved
   to the local storage via `onSaveToStorage` and emitted to observers.

### Suspending Local Storage

Use `addSuspendingLocalStorage()` when the local storage is exposed via
plain suspend functions (e.g. a DAO with `suspend` methods):

```kotlin
private val store = StoreFactory.simpleStoreBuilder<List<Article>>()
    .addSuspendingLocalStorage()
    .build(
        onFetch = remoteArticlesSource::fetchArticles,
        onSaveToStorage = localArticlesSource::saveLocalArticles,
        onLoadFromStorage = localArticlesSource::loadLocalArticles,
    )
```

`onLoadFromStorage` returns `T?` - return `null` when the storage has no
data yet.

### Reactive Local Storage

Use `addReactiveLocalStorage()` when the local storage can be observed as
a `Flow` (Room `@Query` returning `Flow`, DataStore). The store subscribes
to the flow, so **any change in the local storage is automatically pushed
to all store observers**:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<List<Article>>()
    .addReactiveLocalStorage()
    .build(
        onFetch = remoteArticlesSource::fetchArticles,
        onSaveToStorage = localArticlesSource::saveLocalArticles,
        onObserveStorage = localArticlesSource::observeLocalArticles, // () -> Flow<T?>
    )
```

This removes the need for manual cache updates after local writes:

```kotlin
suspend fun delete(article: Article) {
    remoteArticlesSource.delete(article)
    // the local source is reactive -> the store updates automatically
    localArticlesSource.delete(article)
}
```

### Contracts

Instead of separate lambdas, every `build` overload also accepts a single
*contract* interface, which is convenient when one class implements the
whole data access layer:

```kotlin
class ArticlesDataSource : SimpleSuspendingContract<List<Article>> {
    override suspend fun fetch(): List<Article> = api.fetchArticles()
    override suspend fun saveToLocalStorage(data: List<Article>) = dao.save(data)
    override suspend fun loadFromLocalStorage(): List<Article>? = dao.load()
}

private val store = StoreFactory.simpleStoreBuilder<List<Article>>()
    .addSuspendingLocalStorage()
    .build(ArticlesDataSource())
```

Available contracts: `SimpleContract`, `SimpleSuspendingContract`,
`SimpleReactiveContract`, and the query variants `SimpleQueryContract`,
`SimpleQuerySuspendingContract`, `SimpleQueryReactiveContract`. The specific contract
is determined automatically by store builder configuration.

### Local-Only Stores (No Fetcher)

Sometimes there is no remote source at all - the data lives entirely in a
local, reactive storage (a Room `@Query` returning `Flow`, a DataStore, an
in-memory `StateFlow`). In that case call `disableFetcher()` and provide only
an `onObserve` lambda returning the local `Flow<T>`:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<Settings>()
    .disableFetcher()
    .build(onObserve = settingsDataStore::observeSettings) // () -> Flow<Settings>
```

The store subscribes to the flow and exposes its values as `StoreResult`:

- the **first** emission moves the store from `Loading` to `Loaded`;
- every subsequent emission is pushed to all observers automatically;
- there is no remote fetch and **no save step** - writes go straight to your
  local source, which then re-emits through the same flow.

> The observe flow is expected to emit at least once. If it never emits, the
> store stays in `StoreResult.Loading` (there is no fallback fetch).

A contract overload is available as well via `SimpleReactiveNoFetcherContract`:

```kotlin
class SettingsDataSource : SimpleReactiveNoFetcherContract<Settings> {
    override fun observe(): Flow<Settings> = dataStore.observeSettings()
}

private val store = StoreFactory.simpleStoreBuilder<Settings>()
    .disableFetcher()
    .build(SettingsDataSource())
```

`disableFetcher()` can be combined with `withQuery` (in either order) to
observe local data parameterized by a query, using
`SimpleQueryReactiveNoFetcherContract` or an `onObserve: (Q) -> Flow<T>`
lambda:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<List<Note>>()
    .disableFetcher()
    .withQuery(initialQuery = "")
    .build(onObserve = { query -> notesDao.observeNotes(query) })
```

## Queries

`withQuery` turns a `SimpleStore<T>` into a `SimpleQueryStore<Q, T>` whose
`onFetch` (and storage callbacks) receive the current query. Submitting a
new query re-fetches the data:

```kotlin
class GalleryRepository(
    private val remoteSource: RemoteGalleryDataSource,
    private val localSource: LocalGalleryDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<GalleryImage>>()
        .addSuspendingLocalStorage()
        .withQuery(initialQuery = "", debounceMillis = 500)
        .build(
            onFetch = remoteSource::fetchImages,        // suspend (Q) -> T
            onSaveToStorage = localSource::saveImages,  // suspend (Q, T) -> Unit
            onLoadFromStorage = localSource::loadImages, // suspend (Q) -> T?
        )

    fun getImages(): Flow<StoreResult<List<GalleryImage>>> = store.observe()

    fun getQuery(): StateFlow<String> = store.queryFlow

    suspend fun setQuery(query: String) = store.submitQuery(query)
}
```

Key points:

- `debounceMillis` delays the re-fetch so fast typing does not spam the
  data source.
- `queryFlow` is a `StateFlow<Q>` holding the current query - useful for
  rendering the active filter in the UI.
- `submitQuery(query, loadRequest)` suspends until the load triggered by
  the query finishes; `submitQueryAsync` is the fire-and-forget version.
- The default `loadRequest` of `submitQuery` is `LoadRequest.Silent`, so
  the previous content stays visible while results for the new query are
  loading. Pass `LoadRequest.Default` to show the `Loading` state instead.

## Updating Cached Data

`optimisticUpdate` lets you show changes instantly while a long-running
real update is still in progress. Values emitted via `emit` are
**automatically reverted** if the block throws:

```kotlin
suspend fun update(newProfile: UserProfile) {
    store.optimisticUpdate {
        emit(newProfile)                          // visible to observers immediately
        dataSource.updateUserProfile(newProfile)  // real update; reverts in-memory cache on failure
    }
}
```

The lambda receives the current cached value, which makes per-item updates
in lists straightforward:

```kotlin
suspend fun toggleLike(image: GalleryImage) {
    store.optimisticUpdate { oldList ->
        val updated = image.copy(isLiked = !image.isLiked)
        emit(oldList.map { if (it.id == updated.id) updated else it })
        remoteSource.updateImage(updated)
        localSource.updateImage(updated)
    }
}
```

When the real data source has already been updated and you only need to
reflect the change in the cache, use the simpler `update` extension:

```kotlin
suspend fun clear() {
    cartDataSource.clear()
    store.update { emptyList() }
}
```

Both calls are no-ops if there is no loaded value in the cache yet.

To replace the cached result entirely - including switching to a `Loading`
or `Failed` state - use `updateWith`, which sets the given `StoreResult`
into the in-memory cache and emits it to observers immediately:

```kotlin
// push a value loaded elsewhere straight into the cache
store.updateWith(StoreResult.Loaded(profile))

// or surface a failure manually
store.updateWith(StoreResult.Failed(exception))
```

Unlike `update` / `optimisticUpdate`, `updateWith` does not depend on a
previously loaded value and accepts any `StoreResult`.

## Reacting to Store Activity

`whenActive` registers a block that runs while the store is *active* - from
the moment the first observer subscribes until the cache is released. The
block is cancelled automatically when the store becomes inactive. Use it
to keep the store in sync with events from other parts of the app:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<List<Cat>>()
    .build(onFetch = catsDataSource::fetchCats)
    .whenActive {
        catEvents.observeCatEvents().collect { event ->
            optimisticUpdate { oldList ->
                emit(oldList.map { if (it.id == event.cat.id) event.cat else it })
            }
        }
    }
```

`whenActive` returns the store itself, so it can be chained directly after
`build`. See [Keyed Store](keyed-store.md#synchronizing-stores) for a full
master-detail synchronization example.

## API Summary

| Member                                             | Description                                                 |
|----------------------------------------------------|-------------------------------------------------------------|
| `observe(request)`                                 | Observe the cached value as `Flow<StoreResult<T>>`          |
| `get()`                                            | Read the latest `StoreResult<T>` synchronously              |
| `invalidate(request)` / `invalidateAsync(request)` | Force a reload                                              |
| `optimisticUpdate { }`                             | Update the cache ahead of the real update, with auto-revert |
| `update { }`                                       | Plain cache update (extension on top of `optimisticUpdate`) |
| `updateWith(storeResult)`                          | Replace the cached result with any `StoreResult`            |
| `whenActive { }`                                   | Run a block while the store has observers                   |
| `queryFlow`, `submitQuery`, `submitQueryAsync`     | Query support (`SimpleQueryStore` only)                     |
