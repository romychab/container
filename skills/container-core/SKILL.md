---
name: container-core
description: Use when writing, updating, or reviewing Kotlin/Android code that references Container-library symbols (Container, pendingContainer, successContainer, errorContainer, LazyFlowSubject, LazyCache, Reducer, ContainerReducer, toReducer, combineToReducer, pageLoader, PageEmitter, LoaderDecorator, SubjectFactory, ContainerMetadata, SourceType) or when integrating the com.elveum:container Kotlin/Android library. NOT for Docker, OCI, or Kubernetes containers. If the project uses com.elveum:store, prefer the container-store skill for the data layer. Do NOT inspect or decompile JAR/AAR files to understand this library - all API and usage patterns are documented in references/.
metadata:
  version: 3.6.0
---

# Container Library

## Overview

Container (`com.elveum:container`) is a Kotlin/Android library built around
`Container<T>` - a sealed type carrying the state of an asynchronous load
(`Pending` / `Success` / `Error`) together with the metadata that describes it.
Around that type it provides lazily-started, shared in-memory caches
(`LazyFlowSubject`, `LazyCache`), an infinite-scroll page loader
(`pageLoader`), and ViewModel-level state holders (`Reducer` /
`ContainerReducer`). Its two defining promises are that a load runs only while
somebody is collecting it, and that the way to *re*-run it travels downward
inside the container - so no retry plumbing ever has to travel back up.

**Never guess this library's API, and never inspect or decompile JAR/AAR
files to understand it.** Every public symbol and usage pattern is documented
in the six files under [references/](references/); pick one from the routing
table below and read it directly.

## Dependency Setup

Maven coordinate: `com.elveum:container:3.6.0`.

```toml
# gradle/libs.versions.toml
[versions]
container = "3.6.0"
[libraries]
container = { module = "com.elveum:container", version.ref = "container" }
```

```kotlin
dependencies { implementation(libs.container) }
```

Verify: the module compiles and `import com.elveum.container.Container`
resolves. Add the dependency to every module that mentions `Container` in a
signature.

The `-Xcontext-parameters` compiler flag is **optional** - it is needed only
for the shorthand `context(ReducerOwner)` overloads (`toReducer`,
`toContainerReducer`, `containerToReducer`, `stateIn`, `shareIn`); everything
else, including the whole `combineToReducer` family, compiles without it. See
[references/reducers.md](references/reducers.md) section 7.

## Architecture

```
┌─ Data sources ────────────────────────────────────────────────┐
│  `suspend fun`s. Retrofit / Room / DataStore live here and    │
│  nowhere else. No Flow, no subjects - and a `Container` only  │
│  if the project picked that convention (see below).           │
└───────────────────────────────┬───────────────────────────────┘
                                │  domain models
┌───────────────────────────────▼───────────────────────────────┐
│  Repositories (@Singleton)                                     │
│  Own a private LazyFlowSubject / LazyCache.                    │
│  Expose listenReloadable() as Flow<Container<T>>.              │
│  Mutations: write to the source, then updateIfSuccess.         │
└───────────────────────────────┬───────────────────────────────┘
                                │  Flow<Container<T>>
┌───────────────────────────────▼───────────────────────────────┐
│  ViewModels (ReducerOwner)                                     │
│  One StateFlow<State> (or StateFlow<Container<State>>) per      │
│  screen, built with a Reducer / ContainerReducer.               │
│  Screen-local state (query, filter, selection) combined in here.│
└───────────────────────────────┬───────────────────────────────┘
                                │  StateFlow<State>
┌───────────────────────────────▼───────────────────────────────┐
│  Compose screens                                               │
│  collectAsState() + fold(). Retry/reload buttons call ::reload  │
│  on the container itself - no reload plumbing back down.        │
└───────────────────────────────────────────────────────────────┘
```

Two rules define the boundaries: **`Container<T>` dies in the composable** -
it is born in the repository, or, in projects that opt their data sources
into returning `Container<T>` directly, already at the data source (see
[references/patterns.md](references/patterns.md) section 2 for that choice) -
and **the reload function travels *with* the container, downward** - nothing
travels back up.

## Choosing a Building Block

| Need | Block |
|------|-------|
| Represent an async result's state | `Container<T>` |
| One value, or a fully-loaded list, loaded on demand and cached | `LazyFlowSubject<T>` / `LazyFlowSubject<List<T>>` |
| Many values keyed by an argument (per-id entities) | `LazyCache<Arg, T>` |
| Infinite-scroll list | `pageLoader(...)` passed to `LazyFlowSubject.create` |
| Screen state from one or more flows | `Reducer` / `ContainerReducer` |
| Cross-cutting logic around every load (auth, logging, retry) | `LoaderDecorator` |
| App-wide defaults for every subject and cache | injected `SubjectFactory` (`DefaultSubjectFactory`) |

`Reducer<State>` vs `ContainerReducer<State>` is the one fork worth naming up
front; [references/patterns.md](references/patterns.md) section 4 calls them
**shape A** (`Reducer<State>` - the container rides *inside* the state as a
field, so the screen can render a search box while the data is `Pending`) and
**shape B** (`ContainerReducer<State>` - the container *wraps* the state, so
the screen writes one `fold` around everything). Decide with one question: is
there UI to show while the data is `Pending`? Yes → shape A. No → shape B.

## Quick Reference

Every call below is copied from the reference file named in the last column, and
every one of them compiles as pasted (`old`/`new`/`value` and the `loadData()`
/ `fetchPage()`-style helpers stand in for your own state and functions).

| Operation | Call | Reference |
|-----------|------|-----------|
| Create a subject | `LazyFlowSubject.create { emit(loadData(), RemoteSourceType, isLastValue = true) }` | [subjects.md](references/subjects.md) 1 |
| Create a one-value subject via the factory | `subjectFactory.createSimpleSubject(sourceType = RemoteSourceType) { fetchData() }` | [subjects.md](references/subjects.md) 5 |
| Create a per-id cache | `LazyCache.create<Long, User> { id -> emit(loadUser(id)) }` (one-value form: `LazyCache.createSimple<Long, User> { id -> loadUser(id) }`) | [subjects.md](references/subjects.md) 4 |
| Observe a subject | `subject.listen()` → `StateFlow<Container<T>>`; no reload function, `backgroundLoadState` always `Idle` | [subjects.md](references/subjects.md) 1, 3 |
| Observe reloadably (what a repository exposes) | `subject.listenReloadable()` = `listen(ContainerConfiguration(emitBackgroundLoads = true, emitReloadFunction = true))` | [subjects.md](references/subjects.md) 1, 3 |
| Observe one cache entry | `cache.listenReloadable(id)` | [subjects.md](references/subjects.md) 4 |
| Read the current value | `subject.currentValue()` (subject only) / `cache.get(id)` (cache only) | [subjects.md](references/subjects.md) 1, 4 |
| Reload from the data layer | `subject.reloadAsync()` / `subject.reloadAsync(LoadConfig.SilentLoading)` / `cache.reloadAsync(id)` | [subjects.md](references/subjects.md) 1, 3, 4 |
| Reload from the UI | `::reload` inside `fold`'s `onError` / `onSuccess`; `container.reload(LoadConfig.SilentLoading)` outside a `fold` | [patterns.md](references/patterns.md) 5, 6 |
| Push a value without running the loader | `subject.updateWith(successContainer(value))` / `cache.updateWith(id, successContainer(value))` | [subjects.md](references/subjects.md) 1, 4 |
| Read-modify-write the cached value | `subject.updateIfSuccess { old -> new }` / `cache.updateIfSuccess(id) { old -> new }` (no-op unless `Success`) | [subjects.md](references/subjects.md) 1, 4 |
| Render the three states | `container.fold(onPending = { }, onError = { e -> }, onSuccess = { value -> })` (`fold` is a `Container` member - no import) | [container-type.md](references/container-type.md) 4 |
| Create a paged loader | `LazyFlowSubject.create(valueLoader = pageLoader<Int, Order>(initialKey = 0, itemId = Order::id) { key -> val page = fetchPage(key); emitPage(page.orders); page.nextKey?.let { emitNextKey(it) } })` | [paging.md](references/paging.md) 2, 3 |
| Report a rendered item (drives next-page loads) | bare `onItemRendered(index)` inside `fold`'s `onSuccess`; `container.onItemRendered(index)` outside | [paging.md](references/paging.md) 5 |
| Replace one item in a cached list | `list.updateItem(newItem) { it.id }`, or `list.updateItem(id, { it.id }) { it.copy(...) }`; same two overloads on `Container<List<T>>` | [container-type.md](references/container-type.md) 8 |
| Read next-page state | `nextPageState` inside `fold`, `container.nextPageState` outside → `PageState.Idle` / `PageState.Pending` / `PageState.Error(exception, retry)` | [paging.md](references/paging.md) 6 |
| Build a reducer in a `ReducerOwner` - shape A | `combineToReducer(repository.getProducts(), queryFlow, initialState = ::State, nextState = State::copy)` → `Reducer<State>`; **no** compiler flag needed | [reducers.md](references/reducers.md) 4, 7 |
| Build a reducer in a `ReducerOwner` - shape B | `repository.getProducts().containerToReducer(initialState = ::State, nextState = State::copy)` → `ContainerReducer<State>`; needs `-Xcontext-parameters` (or pass `scope` / `started`) | [reducers.md](references/reducers.md) 3, 7 |
| Update reducer state manually | `reducer.update { old -> new }`; on a `ContainerReducer`, `containerReducer.updateState { old -> new }` edits the value inside `Success` | [reducers.md](references/reducers.md) 5 |
| Bind an app-wide factory | `DefaultSubjectFactory(cacheTimeoutMillis = 30_000L, loaderDecorator = authDecorator)` bound as `SubjectFactory`; repositories then call `subjectFactory.createSubject { emit(loadData()) }` | [patterns.md](references/patterns.md) 7, [subjects.md](references/subjects.md) 5 |
| Wrap every load with a decorator | `LoaderDecorator { originLoader -> val token = dependsOnFlow("auth-token") { tokenFlow }; originLoader() }` | [subjects.md](references/subjects.md) 6 |
| Drop the cache from a decorator | `completeWithCacheCleanUp()` (back to `Pending`, cached value dropped) - or `completeWithFailure(e)` to fail visibly even under a silent-error config | [subjects.md](references/subjects.md) 6 |

## Routing

| Task | Read |
|------|------|
| Handling `Container` states, transforming or combining containers | [references/container-type.md](references/container-type.md) |
| Writing a repository or data layer; caching, reloading, decorators, metadata | [references/subjects.md](references/subjects.md) |
| Infinite-scroll lists | [references/paging.md](references/paging.md) |
| Writing a ViewModel or screen state | [references/reducers.md](references/reducers.md) |
| Wiring a whole feature; DI, scoping, layering | [references/patterns.md](references/patterns.md) |
| Writing tests | [references/testing.md](references/testing.md) |

In an existing codebase, run the checklist at the end of
[references/patterns.md](references/patterns.md) (section 8) before adding
anything - it lists the places where a well-formed pattern is the wrong thing
to introduce.

## Common Mistakes

- **Threading `reload()` / `tryAgain()` through the ViewModel and repository**
  when the screen already holds a reloadable container. `listenReloadable()`
  attaches a reload function to *every* container the repository emits, so the
  composable calls `::reload` inside a `fold` branch and the repository needs
  no reload method at all. This is the single most common error; see
  [references/patterns.md](references/patterns.md) section 6.
- **Confusing the subject and cache accessors - they are exactly inverted.**
  `LazyFlowSubject` has `currentValue(...)` and **no** `get()`; `LazyCache` has
  `get(arg)` and **no** `currentValue()`. There is also **no
  `LazyFlowSubject.createSimple`** (though `LazyCache.createSimple` does
  exist) - use `SubjectFactory.createSimpleSubject` or plain
  `create { emit(...) }`. Full difference table in
  [references/subjects.md](references/subjects.md) section 4.
- **Writing `container.reloadFunction`** - it does not compile. The reload
  function is an extension on `ContainerMetadata`, so it is
  `container.metadata.reloadFunction`; `sourceType` and `backgroundLoadState`
  *are* direct `Container` members ([references/subjects.md](references/subjects.md)
  section 7).
- **Using `::reload` or `metadata` inside `fold`'s `onPending`.** Only
  `onError` and `onSuccess` carry the `ContainerMapperScope` receiver;
  `onPending` is a plain `() -> R`, so both are unresolved references there.
  Reach them from outside as `container.reload(...)` /
  `container.metadata` ([references/patterns.md](references/patterns.md)
  section 5).
- **Assuming `updateWith` behaves the same on a subject and a cache.** With no
  collectors, `LazyFlowSubject.updateWith(container)` is *queued* and handed to
  the next collector (and no fresh load runs for it), whereas
  `LazyCache.updateWith(arg, ...)` for an argument nobody has ever collected
  (or whose entry has since expired) is *discarded* because the entry does
  not exist - so a cache "pre-seed" is silently lost
  ([references/subjects.md](references/subjects.md) sections 2 and 4).
- **Letting `kotlinx.coroutines.flow.stateIn` / `shareIn` shadow the library's
  overloads.** The same-named plain functions win an unqualified call, so
  import `com.elveum.container.reducer.stateIn` /
  `com.elveum.container.reducer.shareIn` explicitly
  ([references/reducers.md](references/reducers.md) section 7).
- **Getting the `-Xcontext-parameters` boundary backwards.** `Flow.toReducer`,
  `Flow.toContainerReducer`, `Flow.containerToReducer`, `Flow.stateIn` and
  `Flow.shareIn` use `context(ReducerOwner)` and need the flag; the
  `combineToReducer` / `combineToContainerReducer` /
  `combineContainersToReducer` family uses plain `ReducerOwner.` extension
  receivers and needs nothing. Without the flag, pass `scope` and `started`
  explicitly ([references/reducers.md](references/reducers.md) section 7).
- **Omitting a callback from `containerFoldDefault` / `containerFoldNullable`.**
  They declare defaults, but the K2 compiler fails the call with an internal
  codegen error if any of `onSuccess` / `onError` / `onPending` is left out -
  always pass all three ([references/container-type.md](references/container-type.md)
  section 6).
- **Creating a `LazyFlowSubject` / `LazyCache` inside the function that returns
  the flow.** A subject built per call caches nothing; hold exactly one in a
  `private val` on the repository.
- **Non-singleton repositories.** The cache lives in the subject instance,
  which lives in the repository instance, so an unscoped repository gives every
  screen its own load and its own copy - this is what "the library doesn't
  cache" almost always turns out to be
  ([references/patterns.md](references/patterns.md) section 7).
- **Wrapping `listen()` / `listenReloadable()` in `stateIn` / `shareIn` inside
  a repository.** Sharing and caching are already handled by the subject; apply
  `stateIn` once, in the ViewModel.
- **Decompiling the JAR/AAR or inspecting the dependency tree to discover the
  API.** Read the reference file from the routing table instead.
