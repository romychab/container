---
name: container-store
description: Use when writing, updating, or reviewing any code that references Store-related symbols (StoreFactory, SimpleStore, KeyedStore, PagedStore, PagedQueryStore, SimpleQueryStore, StoreResult, LoadRequest, StoreResultReducer) or when integrating the com.elveum:store Kotlin/Android library. Do NOT inspect or decompile JAR/AAR files to understand this library - all API and usage patterns are documented in references/api.md and references/patterns.md.
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
directly — no decompilation or dependency tree inspection needed.

## Dependency Setup

Maven coordinates: `com.elveum:store:3.1.2` (transitively brings
`com.elveum:container`, whose types are part of the public API).

```toml
# gradle/libs.versions.toml
[versions]
store = "3.1.2"
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
| Entities fetched by ID, each cached independently | `KeyedStore<Key, T>` | `StoreFactory.keyedStoreBuilder<Key, T>()` |
| Infinite-scroll list loaded in pages | `PagedStore<T>` | `StoreFactory.pagedStoreBuilder<PageKey, T>(initialKey, itemId)` |
| Paged list parameterized by query/filter | `PagedQueryStore<Q, T>` | `pagedStoreBuilder(...).withQuery(initialQuery)` |

Rules: data identified by an ID observed from different screens → keyed.
List that grows while scrolling → paged; fully loaded list → simple store
of `List<T>`. `withQuery` is for runtime parameters that re-trigger
fetching (search text, filters) - not a substitute for keyed stores.

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

## Quick Reference

| Operation | Call |
|-----------|------|
| Observe | `store.observe()` / `store.observe(key)` → `Flow<StoreResult<T>>` |
| Read latest result synchronously | `store.get()` / `store.get(key)` → `StoreResult<T>` |
| Read latest value/error synchronously | `store.getOrNull()` / `store.failureOrNull()` (key variants for keyed) |
| Local-only store (no remote fetcher) | `builder.disableFetcher().build(onObserve = { ... })` (simple & keyed) |
| Pagination: total item count | `PagedList(items, nextKey, totalCount = n)`; read `result.metadata.totalPagedItemsCount` |
| Await first completed result | `store.observe().firstGetOrThrow()` (suspend; value or throws) |
| Silent refresh (pull-to-refresh) | `store.invalidateAsync(LoadRequest.Silent)` |
| Non-silent reload (try-again) | `store.invalidateAsync()` |
| Update cache after real write | `store.update { new }` (suspend extension) |
| Replace cached result outright | `store.updateWith(StoreResult.Loaded(new))` / `store.updateWith(key, ...)` |
| Optimistic update (auto-revert on failure) | `store.optimisticUpdate { old -> emit(new); realUpdate() }` |
| Submit query | `store.submitQueryAsync(query)` (default `LoadRequest.Silent`) |
| Pagination: report visible item | `store.onItemRendered(index)` |
| Pagination: next-page status | `result.nextPageState` (`Idle`/`Pending`/`Error(retry)`) |
| React while store is observed | `.whenActive { ... }` (chain after `build`) |

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

- Guessing imports - the two `update` extensions live in different
  packages (`stores.base` for simple/paged, `stores.keyed` for keyed).
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
