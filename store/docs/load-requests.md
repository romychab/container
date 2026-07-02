# Load Requests

`LoadRequest` configures how a store loads data: which sources are used
and how the loading state is propagated to observers. A request is
supplied in exactly two places: per observer via `observe(request)`, and
as the store-wide default via `setLoadRequest(...)` on the builder.
Mutating and query calls - `invalidate` / `invalidateAsync` and
`submitQuery` / `submitQueryAsync` - do **not** take a request; they just
trigger a reload, and every observer keeps receiving data according to the
request it subscribed with.

## Table of Contents

- [Predefined Requests](#predefined-requests)
- [Building a Custom Request](#building-a-custom-request)
  - [Source Modes](#source-modes)
  - [Keeping Content](#keeping-content)
- [Observing Background Loads](#observing-background-loads)
- [Where Load Requests Are Accepted](#where-load-requests-are-accepted)

## Predefined Requests

| Request               | Behaviour                                                                                                                                                                       |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LoadRequest.Default` | Fetch only on demand, when the value is not in the in-memory cache. While loading, observers see `StoreResult.Loading`. Two identical concurrent requests fetch data only once. |
| `LoadRequest.Silent`  | The same, but existing content stays visible while new content is being loaded (equivalent to `builder().keepContentOnLoad().build()`).                                         |

`LoadRequest.Silent` is the natural choice for pull-to-refresh - configure
it as the observer's request so that a reload keeps the existing content
visible:

```kotlin
// as the store-wide default on the builder:
StoreFactory.simpleStoreBuilder<Data>()
    .setLoadRequest(LoadRequest.Silent)
    .build { /* ... */ }

// or per observer:
fun observe() = store.observe(LoadRequest.Silent)
```

The reload itself can be triggered straight from the emitted result via
`result.invalidate()` (see [Store Results](store-results.md)), so the
repository/ViewModel usually does **not** need a dedicated `refresh()` /
`tryAgain()` function - the UI reloads the same store instance that produced
the result:

```kotlin
PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = result::invalidate,          // no ViewModel function needed
) { /* content */ }
```

`LoadRequest.Default` fits a screen that wants the `Loading` state shown
while it reloads (e.g. a "try again" button after a failed load); it is the
default when no request is configured. The failed result reloads itself the
same way:

```kotlin
Button(onClick = result::invalidate) { Text("Try again") } // Default -> Loading shown
```

If you do need to trigger a reload without a result at hand,
`store.invalidateAsync()` still works; it is just no longer required for the
common pull-to-refresh / try-again flows.

## Building a Custom Request

`LoadRequest.builder()` provides fine-grained control:

```kotlin
val request = LoadRequest.builder()
    .freshMode()                 // optional: skip caches, force a remote fetch
    .keepContentOnLoadAndError() // optional: keep old content during load and on failure
    .build()

// use it as the store-wide default:
StoreFactory.simpleStoreBuilder<Data>()
    .setLoadRequest(request)
    .build { /* ... */ }

// or per observer:
store.observe(request)
```

### Source Modes

The source mode determines where data is loaded from
(`LoadRequestSource`):

| Mode      | Builder call    | Behaviour                                                                                                                                   |
|-----------|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `Default` | *(none)*        | Use the in-memory cache if available; fetch only when there is no cached value                                                              |
| `Fresh`   | `freshMode()`   | Ignore all cached values and fetch from the remote source                                                                                   |
| `Offline` | `offlineMode()` | Use only cached values; never hit the remote source. If no cached data exists, `NoCachedDataException` is emitted as a `StoreResult.Failed` |

### Keeping Content

The keep-content options control what observers see while a new value is
being loaded:

- **`keepContentOnLoad()`** - the previously loaded content stays in all
  observed flows while the new content is loading. If the load fails, the
  failure replaces the content.
- **`keepContentOnLoadAndError()`** - the previous content also survives a
  failed load; the UI keeps showing the old data instead of an error
  screen.

Without either option, observers see `StoreResult.Loading` for the
duration of the load.

Both keep-content options accept an optional
`replaceErrorsOnReload: Boolean = true` parameter that controls what happens
when the **current** state is an *error* and a reload starts:

- **`replaceErrorsOnReload = true`** (default) - a current error is replaced
  by a loading state on reload, i.e. content is kept silently only while it is
  actually loaded; a stale error is not.
- **`replaceErrorsOnReload = false`** - a current error is also kept silently
  during the reload (the previous error stays visible with a background-load
  indicator instead of switching to `StoreResult.Loading`).

```kotlin
// keep loaded content silently, but show a loading state when reloading after an error
val request = LoadRequest.builder().keepContentOnLoad().build()

// keep even a previous error visible while reloading
val request = LoadRequest.builder().keepContentOnLoad(replaceErrorsOnReload = false).build()
```

## Observing Background Loads

When content is kept on load, the reload happens "behind" the visible
data. Its progress is reported via metadata instead of a state change:

```kotlin
// completed result + background reload in progress:
val isRefreshing = result.isBackgroundLoading()

// raw access:
val state: BackgroundLoadState = result.backgroundLoadState
```

This is how a pull-to-refresh spinner is typically driven - the spinner reads
`isBackgroundLoading()` and the refresh gesture reloads the result directly:

```kotlin
PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = result::invalidate,
) { /* content */ }
```

## Where Load Requests Are Accepted

A `LoadRequest` is accepted in only two places:

| Location                            | Signature                                            | Notes                                                                      |
|-------------------------------------|------------------------------------------------------|----------------------------------------------------------------------------|
| `observe`                           | `observe(request: LoadRequest? = null)`              | The request this observer subscribes with; `null` uses the store default   |
| Builder `setLoadRequest`            | `setLoadRequest(LoadRequest)` / `setLoadRequest(Flow<LoadRequest>)` | The store-wide default request applied when an observer passes `null`      |

Passing `null` to `observe` (or omitting the parameter) falls back to the
store's configured default request, which is `LoadRequest.Default` unless
overridden via `setLoadRequest(...)` on the store builder. Keyed stores take
the key as the first parameter: `store.observe(key, request)`.

The mutating and query operations - `invalidate` / `invalidateAsync` and
`submitQuery` / `submitQueryAsync` (and their keyed variants) - do **not**
accept a `LoadRequest`.

### Reactive default request

`setLoadRequest` has two overloads:

- `setLoadRequest(loadRequest: LoadRequest)` - a fixed default request.
- `setLoadRequest(flow: Flow<LoadRequest>)` - a reactive stream whose latest
  emission becomes the current default. Whenever the flow emits a new request,
  it becomes the policy applied to every subsequent load, without recreating
  the store or touching each `observe(...)` call site.

This overload is the right tool whenever the loading policy depends on state
that changes at runtime. Typical use cases:

- **Automatic online/offline switching** - fall back to `offlineMode()` when
  connectivity drops and back to `Default` when it returns.
- **A user-controlled "offline mode" flag** stored somewhere (DataStore,
  preferences, a settings table) and toggled from a settings screen - the flag
  flow is mapped into a `LoadRequest`, so flipping the toggle immediately
  changes how the store loads.
- **A "data saver" / metered-connection setting** that prefers cached data,
  or a **battery-saver** mode that avoids fresh fetches.

Because the flow drives the *default* request, the change applies to reloads
triggered later (`invalidate` / `invalidateAsync`, `result.invalidate()`) and
to new observers - no request needs to be passed at those call sites.

Automatic switching driven by connectivity:

```kotlin
StoreFactory.simpleStoreBuilder<Data>()
    .setLoadRequest(
        connectivity.isOnline.map { online ->
            if (online) LoadRequest.Default
            else LoadRequest.builder().offlineMode().build()
        }
    )
    .build { /* ... */ }
```

A user-controlled offline flag persisted in a settings store - toggling it in
the UI changes the store's loading policy on the fly:

```kotlin
class ArticlesRepository(
    storeFactory: StoreFactory,
    settings: SettingsStore,               // exposes the persisted flag as a Flow
    articlesDataSource: ArticlesDataSource,
) {

    private val store = storeFactory.simpleStoreBuilder<List<Article>>()
        .addSuspendingLocalStorage()       // so offlineMode() has cached data to serve
        .setLoadRequest(
            settings.offlineModeEnabled.map { offline ->   // Flow<Boolean> from DataStore
                if (offline) LoadRequest.builder().offlineMode().build()
                else LoadRequest.Default
            }
        )
        .build(articlesDataSource)

    fun getArticles(): Flow<StoreResult<List<Article>>> = store.observe()
}
```

### Why invalidation takes no request

A store (or a single key) may have several observers at once, each
subscribed with its own request - one fresh, one keeping content, another
offline. Invalidation therefore does not carry a request: it simply
triggers a reload, and every observer keeps receiving data according to the
request **it** subscribed with via `observe(...)` (or the store default).
If invalidation carried a request it would override each observer's own
policy, which is why the request lives on `observe` and the builder instead.
