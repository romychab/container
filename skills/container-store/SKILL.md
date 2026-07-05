---
name: container-store
description: Use when writing, updating, or reviewing any code that references Store-related symbols (StoreFactory, SimpleStore, KeyedStore, KeyedQueryStore, PagedStore, PagedKeyedStore, PagedQueryStore, SimpleQueryStore, StoreResult, LoadRequest, StoreResultReducer) or when integrating the com.elveum:store Kotlin/Android library. Do NOT inspect or decompile JAR/AAR files to understand this library - all API and usage patterns are documented in references/api.md and references/patterns.md.
---

# Container Store Library

## Overview

Container Store (`com.elveum:store`) is a Kotlin library for caching and observing
data. A *store* wraps data sources (network, database) into one object
that loads data on demand, caches it in memory while observers are
subscribed, optionally syncs it with a local storage, and exposes it as
`Flow<StoreResult<T>>` (`Loading` / `Loaded` / `Failed`).

**Never guess this library's API, and never inspect JAR/AAR files to
understand it.** All public types and imports are documented in
[references/api.md](references/api.md). Layer-by-layer code patterns
(data sources, repositories, ViewModels, Compose screens, DI) are in
[references/patterns.md](references/patterns.md). Read those files
directly - no decompilation or dependency tree inspection needed.

## Dependency Setup

Maven coordinates: `com.elveum:store:3.3.0` (transitively brings
`com.elveum:container`, whose types are part of the public API).

```toml
# gradle/libs.versions.toml
[versions]
store = "3.3.0"
[libraries]
store = { module = "com.elveum:store", version.ref = "store" }
```

```kotlin
dependencies { implementation(libs.store) }
```

Verify: the module compiles and `import com.elveum.store.StoreFactory`
resolves. Add the dependency to every module that references
`StoreResult` (e.g. a separate view-model module).

## Choosing a Store Type

| Use case | Store | Builder |
|----------|-------|---------|
| One value: profile, settings, cart, non-paged list | `SimpleStore<T>` | `StoreFactory.simpleStoreBuilder<T>()` |
| One value parameterized by search/filter | `SimpleQueryStore<Q, T>` | `simpleStoreBuilder<T>().withQuery(initialQuery)` |
| Entities fetched by ID, each cached independently | `KeyedStore<Key, T>` | `StoreFactory.simpleStoreBuilder<T>().withKeys<Key>()` |
| Per-key value parameterized by query/filter | `KeyedQueryStore<Key, Q, T>` | `simpleStoreBuilder<T>().withKeys<Key>().withQuery(initialQuery)` |
| Infinite-scroll list loaded in pages | `PagedStore<T>` | `StoreFactory.pagedStoreBuilder<PageKey, T>(initialKey, itemId)` |
| Paged list parameterized by query/filter | `PagedQueryStore<Q, T>` | `pagedStoreBuilder(...).withQuery(initialQuery)` |
| Independent paged list per key | `PagedKeyedStore<Key, T>` | `pagedStoreBuilder(...).withKeys<Key>()` |
| Per-key paged list parameterized by query | `PagedKeyedQueryStore<Key, Q, T>` | `pagedStoreBuilder(...).withKeys<Key>().withQuery(initialQuery)` |

Rules: data identified by an ID observed from different screens → keyed.
List that grows while scrolling → paged; fully loaded list → simple store
of `List<T>`. `withQuery` is for runtime parameters that re-trigger
fetching (search text, filters) - not a substitute for keyed stores.

`withQuery` has **two forms** (full signatures in
[references/api.md](references/api.md), "Queries (withQuery)"):

- **Imperative** - `withQuery(initialQuery, debounceMillis = 0)`: the store owns
  the query and exposes `queryFlow` + `submitQuery(...)` / `submitQueryAsync(...)`.
  Returns a `*QueryStore` (e.g. `SimpleQueryStore`).
- **External flow** - `withQuery(debounceMillis = 0) { stateFlow }` or
  `withQuery(initialQuery, debounceMillis = 0) { flow }`: the caller's
  `Flow`/`StateFlow` owns the query; the store follows it and stays **plain**
  (`SimpleStore`, no `submitQuery`). Keyed lambdas receive the key:
  `withQuery { key -> flowFor(key) }`. Order-independent with local-storage /
  `disableFetcher()` / `withKeys()` transitions.

## Target Architecture

```
@Composable screen   - collectAsState, render StoreResult via `when`
   ▼
ViewModel            - ONE StateFlow per screen; stateIn / combineStores / StoreResultReducer
   ▼
Repository           - creates and HOLDS Store instances (scoped, usually singleton)
   ▼
Data sources         - Retrofit/DAO wrappers; may implement Store contracts + mapping
```

Reloads (try-again, pull-to-refresh) usually need **no** path through these
layers: a composable can call `result.invalidate()` on the `StoreResult` it
renders to reload the origin store directly - no `reload()`/`tryAgain()`
function on the ViewModel or repository. Keep the current content visible
during the reload by observing with `LoadRequest.Silent`. Prefer this for new
screens; see "Common Mistakes" for when the old wrapper-function approach is
still appropriate.

## Quick Reference

| Operation                                                    | Call                                                                                                                                                   |
|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Observe                                                      | `store.observe()` / `store.observe(key)` → `Flow<StoreResult<T>>`                                                                                      |
| Read latest result synchronously                             | `store.get()` / `store.get(key)` → `StoreResult<T>`                                                                                                    |
| Read latest value/error synchronously                        | `store.getOrNull()` / `store.failureOrNull()` (key variants for keyed)                                                                                 |
| Local-only store (no remote fetcher)                         | `builder.disableFetcher().build(onObserve = { ... })` (simple & keyed)                                                                                 |
| Pagination: total item count                                 | `PagedList(items, nextKey, totalCount = n)`; read `result.totalPagedItemsCount` (`-1` if unknown; shortcut for `result.metadata.totalPagedItemsCount`) |
| Await first completed result                                 | `store.observe().firstGetOrThrow()` (suspend; value or throws)                                                                                         |
| Reload from the UI (try-again / pull-to-refresh) — PREFERRED | `result.invalidate()` on the rendered `StoreResult`; reloads the origin store with no ViewModel/repository plumbing                                    |
| Pull-to-refresh (keep content visible)                       | Observe with `LoadRequest.Silent`, then `result.invalidate()`; progress shows via `result.isBackgroundLoading()`                                       |
| Try-again (reload showing `Loading`)                         | `result.invalidate()` on the failed result (default request re-shows `Loading`)                                                                        |
| Reload without a result at hand (old way, still valid)       | `store.invalidateAsync()` / `store.invalidate()` (key variants for keyed)                                                                              |
| Replace cached result outright                               | `store.updateWith(StoreResult.Loaded(new))` / `store.updateWith(key, ...)`                                                                             |
| Read-modify-write loaded value                               | `store.updateIfSuccess { old -> new }` / `store.updateIfSuccess(key) { old -> new }` (no-op unless `Loaded`)                                           |
| Optimistic update (auto-revert on failure)                   | `store.optimisticUpdate { old -> emit(new); realUpdate() }`                                                                                            |
| Query: store owns it (imperative)                            | `builder.withQuery(initialQuery)` → `store.submitQueryAsync(query)` / `store.queryFlow`                                                                |
| Query: caller owns it (external state flow)                  | `builder.withQuery { stateFlow }` → plain store follows the flow (no `submitQuery`)                                                                    |
| Query: caller owns it (external any flow)                    | `builder.withQuery(initialQuery) { anyFlow }` → plain store follows the flow (no `submitQuery`)                                                        |
| Keys currently active (observed / in cache window)           | `keyedStore.activeKeys` → `StateFlow<Set<Key>>`                                                                                                        |
| Pagination: report visible item                              | `store.onItemRendered(index)`                                                                                                                          |
| Pagination: next-page status                                 | `result.nextPageState` (`Idle`/`Pending`/`Error(retry)`)                                                                                               |
| React while store is observed (connect to other stores/events) | `.whenActive { ... }` chained after `build`; `this` is the store, block runs while observed & is cancelled on cache release (see patterns.md "Relations between stores") |
| Attach/read custom result flags (metadata)                   | `PagedList(items, nextKey, metadata = MyMeta(...))` / `emit(v, metadata = MyMeta(...))`; read `result.metadata.get<MyMeta>()` (see api.md)             |

Cache behavior (all stores): lazy first fetch, one shared in-memory
cache, released after the last observer unsubscribes plus
`setInMemoryCacheTimeout` (default 5 seconds).

## Workflow

1. Read [references/api.md](references/api.md) for exact imports and
   `StoreResult` / `LoadRequest` semantics.
2. Pick the store type from the table above.
3. Read the matching sections of
   [references/patterns.md](references/patterns.md) and follow them for
   the layer you are writing (data source → repository → ViewModel →
   Compose screen, plus DI and scoping).
4. In an existing project, mirror its conventions first - see the
   "Working in an Existing Project" checklist at the end of patterns.md.

## Common Mistakes

- Threading `reload()` / `tryAgain()` / `refresh()` functions through
  ViewModel → repository → store when the UI already holds the `StoreResult`.
  The **preferred** way is to reload the origin store directly from the
  composable with `result.invalidate()` (keep content visible by observing with
  `LoadRequest.Silent`). The wrapper-function approach is still valid - keep it
  when the reload needs extra logic beyond a plain invalidate, when no
  `StoreResult` is available at the call site, or when the project already
  established that convention (do not rewrite existing screens unless asked).
- Using the old `update { }` extension - it was **renamed to `updateIfSuccess`**
  (`store.updateIfSuccess { old -> new }`, keyed: `store.updateIfSuccess(key) { old -> new }`).
  The rename makes explicit that the transform runs **only when the current value is
  `Loaded`**; use `updateWith(StoreResult.Loaded(new))` for an unconditional overwrite.
- Calling `optimisticUpdate`, `updateIfSuccess`, `submitQuery` or `submitQueryAsync` for a
  store/key with **no active observer** - these act on the in-memory cache, which only exists
  while the store (or key) is observed, so the call is silently a no-op. Observe first.
- Faking a fetcher to model a store with **no remote source** (an in-memory
  value, session state, an event-bus stream): `build { awaitCancellation() }`
  (stays `Loading` forever) or `build { Optional.empty() }` (one throwaway value).
  Instead keep the value in a `MutableStateFlow` / `MutableSharedFlow` the app owns
  and observe it with `disableFetcher().build(onObserve = { flow })`; to update,
  mutate the flow directly (no `updateWith` / fake fetch). See patterns.md
  "Externally-driven stores".
- Mishandling mutations (add/update/remove). A mutation always hits the backend;
  what else depends on the local source. **With `addReactiveLocalStorage()`** (or a
  `disableFetcher()` store observing a `Flow`): write to the backend and to the
  local source (Room `@Insert`/`@Update`/`@Delete`) - the local `Flow` re-emits and
  the store updates itself; do **not** also call `updateIfSuccess`/`updateWith`.
  **Without a reactive source** (remote-only or `addSuspendingLocalStorage()`):
  update every layer manually - backend, then suspending storage, then the store
  cache via `updateIfSuccess { }` / `updateWith(...)` (or `optimisticUpdate { }`).
  See patterns.md "Mutations (add / update / remove)".
- Reaching for a `keyedStoreBuilder` - it was removed. Keyed stores are
  created by calling `.withKeys<Key>()` on a simple or paged builder
  (`simpleStoreBuilder<T>().withKeys<Key>()`).
- No reactive local storage for paged stores (`addReactiveLocalStorage()`
  does not exist on paged builders; planned for future versions).
- Wrapping `observe()` in `shareIn`/`stateIn` inside repositories -
  sharing/caching is already handled; apply `stateIn` once, in the
  ViewModel.
- Recreating a repository (and its store) per use - store holders must be
  scoped or the cache is useless.
- Using `ReducerOwner` context overloads in projects without the
  `-Xcontext-parameters` compiler flag - pass `scope`/`started`
  explicitly instead.
- Trying to store a nullable value type - every store generic is `T : Any`,
  so `SimpleStore<User?>` (and keyed/paged equivalents) will not compile.
  Model "value may be absent" as `java.util.Optional<T>` (e.g.
  `SimpleStore<Optional<User>>`): wrap fetch results with `T?.toOptional()`,
  and read them with `getOptionalValueOrNull()` / `firstOptionalGetOrThrow()`
  / `isOptionalEmpty()`.
