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
- [Custom Loaders](#custom-loaders)
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

The builder supports a few base options before `build`:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<UserProfile>()
    .setInMemoryCacheTimeout(60.seconds)  // default: 5 seconds
    .setCoroutineContext(Dispatchers.IO)  // context used by fetch/storage calls
    .setLoadRequest(LoadRequest.Silent)   // default request for observe/invalidate/invalidateAsync
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

Neither takes a [LoadRequest](load-requests.md) - a reload just re-runs the
load, and every observer keeps receiving data according to the request it
subscribed with via `observe(...)` (or the builder default). The most
common pattern is pull-to-refresh, where the old content should stay
visible while reloading; configure that with `LoadRequest.Silent` on the
observer (or as the builder default):

```kotlin
fun getUserProfile() = store.observe(LoadRequest.Silent)
```

That is usually all the repository/ViewModel needs: the emitted result can
reload the store itself via `result.invalidate()`, so the UI drives both
pull-to-refresh and "try again" without a dedicated `refresh()` function. The
in-progress refresh is detected via `result.isBackgroundLoading()`:

```kotlin
PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = result::invalidate,   // no ViewModel refresh function needed
) { /* list content */ }
```

An explicit `store.invalidate()` / `invalidateAsync()` is still available for
cases where you need to reload without a result in hand.

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

## Custom Loaders

When you don't need a full local-storage layer but still want to emit more
than one value per load, use `buildCustom { }` instead of `build(...)`. The
lambda runs as an `Emitter<T>` on each load and can call `emit(...)` several
times - for example a cached value first, then a fresh one:

```kotlin
private val store = StoreFactory.simpleStoreBuilder<UserProfile>()
    .buildCustom {
        // this: Emitter<UserProfile>
        cache.peek()?.let { emit(it) } // show the cached value immediately (optional)
        emit(api.fetchUserProfile())   // then the fresh value
    }
```

`buildCustom` is available on remote-only (no local storage) simple builders.
With `withQuery`, the lambda also receives the current query; the keyed and
paged variants receive the key / page key (see
[Keyed Store](keyed-store.md) and [Paged Store](paged-store.md)).

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
- `submitQuery(query)` suspends until the load triggered by the query
  finishes; `submitQueryAsync` is the fire-and-forget version. Neither
  takes a `LoadRequest` - the loading behaviour follows the request each
  observer subscribed with via `observe(...)` (or the builder default).

### External query flow

Instead of driving the query imperatively with `submitQuery`, you can pass an
existing reactive query stream to `withQuery` as a lambda returning a `Flow`.
The store then follows that flow, and the result is a plain `SimpleStore<T>`
with no `submitQuery`/`queryFlow` of its own - the flow is the single source of
truth for the query:

```kotlin
class GalleryRepository(
    private val remoteSource: RemoteGalleryDataSource,
    private val localSource: LocalGalleryDataSource,
) {

  private val searchQuery = MutableStateFlow<String>("")

  private val store = StoreFactory.simpleStoreBuilder<List<GalleryImage>>()
        .addSuspendingLocalStorage()
        .withQuery(debounceMillis = 500) { searchQuery }
        .build(
            onFetch = remoteSource::fetchImages,       // suspend (Q) -> T
            onSaveToStorage = localSource::saveImages,  // suspend (Q, T) -> Unit
            onLoadFromStorage = localSource::loadImages, // suspend (Q) -> T?
        )

    fun getImages(): Flow<StoreResult<List<GalleryImage>>> = store.observe()
}
```

There are two overloads:

- `withQuery(debounceMillis) { stateFlow }` - for a `StateFlow`, the initial
  query is taken from `StateFlow.value`, so the first load happens immediately.
- `withQuery(initialQuery, debounceMillis) { flow }` - for a plain `Flow` that
  may not emit synchronously, `initialQuery` seeds the immediate first load and
  the flow drives every reload after.

Both are available on the remote-only, suspending, reactive and
`disableFetcher()` (local-only) simple builders. An emission equal to the
current query does not trigger a redundant reload, and the flow is collected
only while the store is active.

`withQuery` is **order-independent** - it can be applied before or after
`addSuspendingLocalStorage()`, `addReactiveLocalStorage()`, `disableFetcher()`
and `withKeys()`, so all of these compile and behave identically:

```kotlin
StoreFactory.simpleStoreBuilder<T>().withQuery { flow }.addSuspendingLocalStorage().build(/* ... */)
StoreFactory.simpleStoreBuilder<T>().addSuspendingLocalStorage().withQuery { flow }.build(/* ... */)
```

Note the two ways to combine an external query with `withKeys` differ in
semantics (see [Keyed Store](keyed-store.md#external-query-flow)):

- `withKeys().withQuery { key -> flowFor(key) }` - **per-key** query flows.
- `withQuery { flow }.withKeys()` - the single flow is **shared by every key**
  (one global query stream driving all keys).

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

`optimisticUpdate` is a no-op if there is no loaded value in the cache yet.
More generally, `optimisticUpdate`, `updateIfSuccess`, `submitQuery` and
`submitQueryAsync` all operate on the **in-memory cache, which exists only
while the store has at least one active observer** (plus the configured
cache timeout after the last observer unsubscribes). Calling them for a store
that is not currently observed does nothing - start observing the store (and
let the value load) before updating or querying it.

When the real data source has already been updated and you only need to apply
a read-modify-write transform to the currently loaded value, use
`updateIfSuccess`. It reads the current value, applies your transform and
writes it back; if the store is not currently holding a loaded value the
transform is not invoked:

```kotlin
suspend fun renameProfile(newName: String) {
    dataSource.renameProfile(newName)
    store.updateIfSuccess { it.copy(name = newName) }
}
```

> `updateIfSuccess` replaced the old `update { }` extension. It was renamed to
> make explicit that the transform runs **only when the current value is
> `Loaded`** - the old name led some callers to assume it always applied.

When the real data source has already been updated and you only need to
reflect the change in the cache regardless of the current state, use
`updateWith`, which sets the given `StoreResult` into the in-memory cache and
emits it to observers immediately:

```kotlin
suspend fun clear() {
    cartDataSource.clear()
    store.updateWith(StoreResult.Loaded(emptyList()))
}
```

`updateWith` can also replace the cached result entirely - including
switching to a `Loading` or `Failed` state:

```kotlin
// push a value loaded elsewhere straight into the cache
store.updateWith(StoreResult.Loaded(profile))

// or surface a failure manually
store.updateWith(StoreResult.Failed(exception))
```

Unlike `optimisticUpdate`, `updateWith` does not depend on a previously
loaded value and accepts any `StoreResult`.

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

| Member                                                           | Description                                                                                    |
|------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `observe(request = null)`                                        | Observe the cached value as `Flow<StoreResult<T>>`; `null` uses the configured default request |
| `get()`                                                          | Read the latest `StoreResult<T>` synchronously                                                 |
| `invalidate()` / `invalidateAsync()`                             | Force a reload (no request argument)                                                           |
| `optimisticUpdate { }`                                           | Update the cache ahead of the real update, with auto-revert (needs an active observer)         |
| `updateIfSuccess { old -> new }`                                 | Read-modify-write the cached value; no-op unless the current value is `Loaded`                 |
| `updateWith(storeResult)`                                        | Replace the cached result with any `StoreResult`                                               |
| `whenActive { }`                                                 | Run a block while the store has observers                                                      |
| `queryFlow`, `submitQuery(query)`, `submitQueryAsync(query)`     | Query support (`SimpleQueryStore` only)                                                        |
