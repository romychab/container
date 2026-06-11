# Container Store - Public API Reference

All public symbols, grouped by package. Use these exact imports; do not
guess others.

## Imports

```kotlin
// Factory - entry point for creating stores
import com.elveum.store.StoreFactory

// Store interfaces (returned by builders; useful for explicit field types)
import com.elveum.store.stores.simple.SimpleStore
import com.elveum.store.stores.simple.SimpleQueryStore
import com.elveum.store.stores.keyed.KeyedStore
import com.elveum.store.stores.keyed.update           // update(key) { } extension for KeyedStore
import com.elveum.store.stores.paged.PagedStore
import com.elveum.store.stores.paged.PagedQueryStore
import com.elveum.store.stores.paged.PagedList        // data class PagedList(items, nextKey)

// Base store members (extensions / supporting types)
import com.elveum.store.stores.base.update            // update { } extension for SimpleStore/PagedStore
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.BaseSimpleStore
import com.elveum.store.stores.base.BasePagedStore
import com.elveum.store.stores.base.WithQuery
import com.elveum.store.stores.base.WithStoreLifecycleOwner

// Results and load requests
import com.elveum.store.load.StoreResult              // sealed: Loading / Loaded / Failed / Completed
import com.elveum.store.load.LoadRequest              // LoadRequest.Default, LoadRequest.Silent, LoadRequest.builder()
import com.elveum.store.load.LoadRequestSource        // enum: Default / Fresh / Offline
import com.elveum.store.load.LoadRequestBuilder

// StoreResult extensions
import com.elveum.store.load.map                      // StoreResult<T>.map { }
import com.elveum.store.load.storeMap                 // Flow<StoreResult<T>>.storeMap { }
import com.elveum.store.load.storeFlatMapResultLatest // inner flow emits StoreResult
import com.elveum.store.load.storeFlatMapLatest       // inner flow emits plain values
import com.elveum.store.load.storeListFlatMapLatest   // list + per-item keyed observation
import com.elveum.store.load.combineStores            // combine 2..5 (or List of) StoreResult flows
import com.elveum.store.load.getOrNull
import com.elveum.store.load.isLoaded                 // smart-cast contract
import com.elveum.store.load.isFailed                 // smart-cast contract
import com.elveum.store.load.isCompleted
import com.elveum.store.load.isForegroundLoading
import com.elveum.store.load.isBackgroundLoading      // true while a silent refresh runs
import com.elveum.store.load.hasAnyLoading
import com.elveum.store.load.nextPageState            // StoreResult<T>.nextPageState: PageState (paged stores)

// Reducers (view-model layer)
import com.elveum.store.reducers.StoreResultReducer
import com.elveum.store.reducers.storeResultToReducer // Flow<StoreResult<T>> -> StoreResultReducer
import com.elveum.store.reducers.toStoreResultReducer // Flow<T>              -> StoreResultReducer

// Contracts (optional; implemented by data sources instead of build(...) lambdas)
import com.elveum.store.contracts.SimpleContract
import com.elveum.store.contracts.SimpleSuspendingContract
import com.elveum.store.contracts.SimpleReactiveContract
import com.elveum.store.contracts.SimpleQueryContract
import com.elveum.store.contracts.SimpleQuerySuspendingContract
import com.elveum.store.contracts.SimpleQueryReactiveContract
import com.elveum.store.contracts.KeyedContract
import com.elveum.store.contracts.KeyedSuspendingContract
import com.elveum.store.contracts.KeyedReactiveContract
import com.elveum.store.contracts.PagedContract
import com.elveum.store.contracts.PagedSuspendingContract
import com.elveum.store.contracts.PagedQueryContract
import com.elveum.store.contracts.PagedQuerySuspendingContract

// Exceptions
import com.elveum.store.exceptions.NoCachedDataException // emitted by offlineMode() without cache

// From the transitive com.elveum:container dependency
import com.elveum.container.subject.paging.PageState  // Idle / Pending / Error(retry)
import com.elveum.container.SourceType                // metadata: where the value came from
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.BackgroundLoadState
import com.elveum.container.reducer.ReducerOwner      // optional base interface for view-models
```

Notes:

- There is **no** reactive local storage for paged stores
  (`PagedReactiveContract` does not exist; paged builders have no
  `addReactiveLocalStorage()`). It is planned for future versions.
- The `update` extension exists in two packages: import
  `com.elveum.store.stores.base.update` for simple/paged stores and
  `com.elveum.store.stores.keyed.update` for keyed stores.
- The zero-arg-context overloads of `storeResultToReducer` /
  `toStoreResultReducer` use Kotlin context parameters
  (`context(owner: ReducerOwner)`); they require the
  `-Xcontext-parameters` compiler flag. The overloads with explicit
  `scope` / `started` parameters work without the flag.

## Core Types

### StoreResult

Every store emits `Flow<StoreResult<T>>` with three states:

```kotlin
StoreResult.Loading                     // no cached data yet; load in progress
StoreResult.Loaded(value, metadata)     // success; value: T
StoreResult.Failed(exception, metadata) // load failed; exception: Exception
// Loaded and Failed share the supertype StoreResult.Completed<T>
```

Useful members on every result: `getOrNull()`, `isLoaded()`,
`isFailed()`, `isBackgroundLoading()`, `sourceType`,
`backgroundLoadState`, and (paged stores only) `nextPageState`.

### LoadRequest

Accepted by `observe`, `invalidate`, `invalidateAsync`, `submitQuery`:

| Request | Effect |
|---------|--------|
| `LoadRequest.Default` | Fetch only if not cached; observers see `Loading` during the load |
| `LoadRequest.Silent` | Keep currently shown content while reloading (pull-to-refresh); progress visible via `isBackgroundLoading()` |
| `LoadRequest.builder()` | Custom: `freshMode()` (skip caches), `offlineMode()` (cache only; emits `NoCachedDataException` if empty), `keepContentOnLoad()`, `keepContentOnLoadAndError()`, then `build()` |

### Store behaviour (all types)

- Lazy: the first observer triggers the fetch; all observers share one
  in-memory cache.
- The cache is released after the last observer unsubscribes plus the
  configured timeout (`setInMemoryCacheTimeout`, default **5 seconds**).
- `setCoroutineContext(Dispatchers.IO)` sets the context for
  fetch/storage callbacks.

## Typical Operations

```kotlin
// Observe (Flow<StoreResult<T>>); lazy, shared, cached:
store.observe()
store.observe(key)                       // keyed store

// Silent refresh (pull-to-refresh): old content stays visible
store.invalidateAsync(LoadRequest.Silent)

// Non-silent reload ("Try again" after an error): shows Loading
store.invalidateAsync()                  // = LoadRequest.Default
suspend fun refresh() = store.invalidate()  // suspend until done

// Plain cache update AFTER the real data source has been changed:
import com.elveum.store.stores.base.update
suspend fun clear() {
    cartDataSource.clear()
    store.update { emptyList() }
}

// Optimistic update: emit the expected value first, then do the real
// update; emitted values are AUTO-REVERTED if the block throws:
suspend fun toggleLike(post: Post) {
    store.optimisticUpdate { oldList ->
        val updated = post.copy(isLiked = !post.isLiked)
        emit(oldList.map { if (it.id == updated.id) updated else it })
        feedDataSource.toggleLike(updated)   // failure -> cache reverted
    }
}

// Keyed variants take the key first:
store.optimisticUpdate(productId) { old -> emit(old.copy(title = title)); api.rename(productId, title) }
import com.elveum.store.stores.keyed.update
store.update(productId) { it.copy(isRead = true) }

// Query stores:
store.queryFlow                          // StateFlow<Q> - current query
store.submitQueryAsync(newQuery)         // re-fetch; default LoadRequest.Silent
suspend fun search(q: Q) = store.submitQuery(q)  // suspends
```
