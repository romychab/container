# Load Requests

`LoadRequest` configures how a store loads data: which sources are used
and how the loading state is propagated to observers. Every loading
operation accepts one - `observe`, `invalidate`, `invalidateAsync` and
`submitQuery` / `submitQueryAsync`.

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

`LoadRequest.Silent` is the natural choice for pull-to-refresh:

```kotlin
fun refresh() {
    store.invalidateAsync(LoadRequest.Silent)
}
```

while `LoadRequest.Default` fits a "try again" button after a failed load,
where showing the `Loading` state is desirable:

```kotlin
fun tryAgain() {
    store.invalidateAsync() // LoadRequest.Default
}
```

## Building a Custom Request

`LoadRequest.builder()` provides fine-grained control:

```kotlin
val request = LoadRequest.builder()
    .freshMode()                 // optional: skip caches, force a remote fetch
    .keepContentOnLoadAndError() // optional: keep old content during load and on failure
    .build()

store.invalidateAsync(request)
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

## Observing Background Loads

When content is kept on load, the reload happens "behind" the visible
data. Its progress is reported via metadata instead of a state change:

```kotlin
// completed result + background reload in progress:
val isRefreshing = result.isBackgroundLoading()

// raw access:
val state: BackgroundLoadState = result.backgroundLoadState
```

This is how a pull-to-refresh spinner is typically driven:

```kotlin
PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = viewModel::refresh,
) { /* content */ }
```

## Where Load Requests Are Accepted

| Operation                          | Default request       | Notes                                                   |
|------------------------------------|-----------------------|---------------------------------------------------------|
| `observe`                          | `LoadRequest.Default` | Applies to the load triggered by this observation       |
| `invalidate` / `invalidateAsync`   | `LoadRequest.Default` | Forces a reload according to the request policy         |
| `submitQuery` / `submitQueryAsync` | `LoadRequest.Silent`  | Previous results stay visible while the new query loads |

Keyed stores take the key as the first parameter:
`store.invalidate(key, request)`, `store.observe(key, request)`.
