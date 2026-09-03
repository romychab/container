# Testing Reference

`com.elveum:container:3.6.0` code is tested with **flow-test**
(`com.uandcode:flowtest:1.1.0`), a small library that replaces
`kotlinx-coroutines-test`'s `runTest { }` / `backgroundScope.launch { }` /
`advanceUntilIdle()` dance with a single `startCollecting()` call. This file
covers flow-test itself, plus the container-library-specific setup every test
of a `LazyFlowSubject`, `LazyCache`, or `Reducer` needs to actually observe
anything.

See [`subjects.md`](subjects.md) for `LazyFlowSubject`, `LazyCache`,
`SubjectFactory`, `LoaderDecorator`, and `LoadConfig`; and
[`reducers.md`](reducers.md) for `Reducer`, `ContainerReducer`, and
`ReducerOwner`. This file assumes both and does not re-explain them.

## 1. Setup

```toml
# gradle/libs.versions.toml
[versions]
flowtest = "1.1.0"
coroutines = "1.10.2"   # or whatever this project already pins
mockk = "1.14.9"

[libraries]
flowtest = { group = "com.uandcode", name = "flowtest", version.ref = "flowtest" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
```

```kotlin
// build.gradle.kts
dependencies {
    testImplementation(libs.flowtest)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)   // for the mocked SubjectFactory in section 5
}
```

> The flow-test coordinate is **`com.uandcode:flowtest:1.1.0`** - group
> `com.uandcode`, artifact `flowtest`.

## 2. `runFlowTest { }` instead of `runTest { }`

The idiomatic way to drive a `kotlinx.coroutines.test.TestScope` by hand is
`runTest { }` plus `backgroundScope.launch { flow.collect { ... } }` plus
`advanceUntilIdle()` to pump the virtual clock. **That combination silently
collects nothing here - and the reason is precise, not vague timing noise:**

```kotlin
// kotlinx.coroutines.test.TestCoroutineScheduler (library source):
public fun advanceUntilIdle(): Unit = advanceUntilIdleOr {
    events.none(TestDispatchEvent::isForeground)
}
```

`advanceUntilIdle()` checks that condition **before** running the next queued
task. `backgroundScope.launch { }` tags its coroutine's dispatch as
*background* work. So if that launch's own first dispatch is the only thing
queued - which it is, the instant after you call it - the condition "every
remaining event is background" is already true, and `advanceUntilIdle()`
refuses to run it at all. The collector coroutine never starts; `collect`'s
body never executes once. This is not a race or a missing `yield()` - it is
what `advanceUntilIdle()` is documented to do, and it reproduces every time:

```kotlin
@Test
fun plainRunTestTrap() = runTest {
    val source = MutableStateFlow(1)
    val collectedItems = mutableListOf<Int>()

    backgroundScope.launch {
        source.collect { collectedItems.add(it) }
    }
    advanceUntilIdle()

    collectedItems.isEmpty() // true - the collector coroutine never ran
}
```

Swapping `advanceUntilIdle()` for `runCurrent()` at that same point *does*
run it (`runCurrent()` has no foreground/background distinction - it runs
every event due at the current virtual time). But the fix that actually
belongs in test code is `runFlowTest { }` plus `startCollecting()`
(section 3): it sidesteps the whole foreground/background distinction by
giving the collector its own `UnconfinedTestDispatcher`, which runs
collection **eagerly, inline, the moment you call it** - no
`backgroundScope`, no manual pumping needed to get the first values.

```kotlin
public fun TestScope.runFlowTest(block: suspend FlowTestScope.() -> Unit)
public fun runFlowTest(block: suspend FlowTestScope.() -> Unit)  // creates its own TestScope
```

Call it as `runFlowTest { }` directly (creates a fresh `TestScope`), or as
`scope.runFlowTest { }` on a `TestScope` you already built in `@Before` (the
library's own test suite does this so `@Before` can wire mocks against the
same `scope` the test body later uses). Either way, the test method itself
still returns `Unit`, same as `runTest { }`.

## 3. `startCollecting()` and `TestFlowCollector<T>`

Inside a `FlowTestScope` block, every `Flow<T>` gets a `startCollecting()`
extension:

```kotlin
public fun <T> Flow<T>.startCollecting(
    context: CoroutineContext = UnconfinedTestDispatcher(scope.testScheduler),
): TestFlowCollector<T>
```

It launches a collector on `UnconfinedTestDispatcher` (by default, sharing
the test's own `testScheduler`) and returns a `TestFlowCollector<T>`:

```kotlin
public interface TestFlowCollector<T> {
    public fun cancel()
    public val collectedItems: List<T>
    public val collectStatus: CollectStatus
    public val count: Int get() = collectedItems.size
    public val hasItems: Boolean get() = count > 0
    public val lastItem: T get() = collectedItems.last()
}
```

```kotlin
val collector = someFlow.startCollecting()

collector.hasItems        // Boolean
collector.count           // Int
collector.lastItem        // T - throws NoSuchElementException if nothing collected yet
collector.collectedItems  // List<T> - the full sequence, in order
collector.collectStatus   // CollectStatus
collector.cancel()        // stop collecting
```

`lastItem` throws if `collectedItems` is empty - guard with `hasItems` first,
or with `collectStatus`, when a flow might not have emitted yet.

`UnconfinedTestDispatcher` is what makes the *first* value available
immediately after `startCollecting()` with **no** `runCurrent()` /
`advanceUntilIdle()` call - a `StateFlow`'s current value (or a value
computed with no suspension before its first `emit`) shows up in
`collectedItems` synchronously. Anything that requires an actual suspending
step first - a real loader coroutine running on a `TestScope`'s own
(non-Unconfined) dispatcher, a `delay()`, a value pushed later - still needs
`runCurrent()` / `advanceTimeBy()` / `advanceUntilIdle()` to observe, exactly
as with plain `runTest { }`. Section 5 shows why this matters immediately for
`LazyFlowSubject`.

## 4. `CollectStatus`, `JobStatus`, `FlowTestScope`

```kotlin
public sealed class CollectStatus {
    public data object Collecting : CollectStatus()
    public data object Completed : CollectStatus()
    public data object Cancelled : CollectStatus()
    public data class Failed(val exception: Exception) : CollectStatus()
}
```

Read via `collector.collectStatus`. `Collecting` while the flow is still
alive and no terminal event has happened; `Completed` once the flow finishes
normally; `Cancelled` after `collector.cancel()`; `Failed(exception)` if
collection threw. Two assertion extensions exist for common cases (import
`com.uandcode.flowtest.assertFailure` / `assertCompleted` /
`assertCollecting`):

```kotlin
collector.assertCompleted()
collector.assertCollecting()
collector.assertFailure(SomeException::class)
collector.assertFailure(expectedException)
```

`FlowTestScope` (the `runFlowTest { }` receiver) also exposes the pieces
`startCollecting()` is built on:

```kotlin
public interface FlowTestScope {
    public val scope: TestScope
    public fun <T> Flow<T>.startCollecting(context: CoroutineContext = /* Unconfined */): TestFlowCollector<T>
    public fun <T> executeInBackground(context: CoroutineContext = /* Unconfined */, command: suspend () -> T): BackgroundCoroutineState<T>
    public fun advanceUntilIdle()
    public fun advanceTimeBy(millis: Long)
    public fun runCurrent()
}
```

- `scope` - the underlying `TestScope`. Needed whenever a `LazyFlowSubject` /
  `LazyCache` / `Reducer` under test wants a `CoroutineScope` or a
  `TestScheduler` (section 5).
- `advanceUntilIdle()` / `advanceTimeBy()` / `runCurrent()` are plain
  forwards to the same-named `TestScope` functions - no foreground/background
  surprise here, because `startCollecting()` never uses `backgroundScope` in
  the first place.
- `executeInBackground` is the flow-test analogue of `startCollecting()` for
  a single `suspend` call instead of a `Flow`; its `BackgroundCoroutineState<T>`
  exposes `status: JobStatus<T>`:

  ```kotlin
  public sealed class JobStatus<out T> {
      public data object Executing : JobStatus<Nothing>()
      public data object Cancelled : JobStatus<Nothing>()
      public data class Completed<out T>(val result: T) : JobStatus<T>()
      public data class Failed(val exception: Exception) : JobStatus<Nothing>()
  }
  ```

  Useful for a suspending call like `newSimpleLoad` (`subjects.md` section 1)
  whose result you want to assert without blocking the test.

## 5. Testing a repository backed by `LazyFlowSubject`

### The `CoroutineScopeFactory` trap - solve this first

`LazyFlowSubject.create`, `LazyCache.create` and `DefaultSubjectFactory` all
default `coroutineScopeFactory` to the library's own companion object:

```kotlin
// CoroutineScopeFactory companion (library source):
override fun createScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
```

A plain JVM unit test has no `Dispatchers.Main` - calling anything that
touches it fails with `IllegalStateException: Dispatchers.Main was accessed
when the platform dispatcher was absent and the test dispatcher was unset`.
This is **not** the flow-test/`runTest` trap from section 2; it fires even
inside a correctly written `runFlowTest { }` test if the subject under test
was built with the default `coroutineScopeFactory`. It reproduces reliably
and is the first thing to fix, or every test in this section throws before
it gets anywhere near an assertion.

### The remedy: inject `SubjectFactory`, not `CoroutineScopeFactory`

The fix is **not** a `CoroutineScopeFactory` parameter on the repository.
`CoroutineScopeFactory` is plumbing for how a subject gets its loading scope;
putting it in a repository constructor leaks that detail into production DI to
serve tests, and still leaves a repository that can only ever build *real*
subjects. Inject a **`SubjectFactory`** instead (`subjects.md` section 5) - the
injection point the library is designed around - and let the kind of test decide
what goes in:

| Kind of test | What you pass as the `SubjectFactory` |
|--------------|----------------------------------------|
| **Unit** - the repository's own logic; no real subject behaviour wanted | a mock |
| **Integration** - the real subject/cache behaviour (loading, caching, reload) | `DefaultSubjectFactory(coroutineScopeFactory = ...)` |

Only the integration case needs a `CoroutineScopeFactory` at all, and it is
handed to `DefaultSubjectFactory`, never to the repository:

```kotlin
private data class User(val id: Long, val name: String)

private class UserRepository(
    subjectFactory: SubjectFactory,
    private val load: suspend () -> User,
) {
    private val subject = subjectFactory.createSubject { emit(load()) }
    fun listenUser(): Flow<Container<User>> = subject.listenReloadable()
    fun reload() = subject.reloadAsync()
}
```

Two helpers, written once per test source set:

```kotlin
fun FlowTestScope.testScopeFactory(): CoroutineScopeFactory =
    CoroutineScopeFactory { TestScope(scope.testScheduler) }

fun FlowTestScope.testSubjectFactory(
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    loaderDecorator: LoaderDecorator = LoaderDecorator,
): SubjectFactory = DefaultSubjectFactory(
    cacheTimeoutMillis = cacheTimeoutMillis,
    coroutineScopeFactory = testScopeFactory(),
    loaderDecorator = loaderDecorator,
)
```

`testSubjectFactory()` binds every subject the factory creates to the test's own
scheduler, so the production repository needs no test-only constructor
parameter. Everything below constructs `UserRepository` with it.

### Unit test: a mocked `SubjectFactory`

When the assertion is about the repository's own code - what it exposes, how it
maps - the subject is a mock, no loading scope is ever created, and the trap
above cannot fire:

```kotlin
@Test
fun listenUserExposesTheSubjectsContainers() = runFlowTest {
    val subject = mockk<LazyFlowSubject<User>>()
    val subjectFactory = mockk<SubjectFactory>()
    every {
        subjectFactory.createSubject<User>(any(), any(), any(), any(), any(), any(), any())
    } returns subject
    every { subject.listen(any()) } returns MutableStateFlow(successContainer(User(1, "Ann")))

    val repository = UserRepository(subjectFactory) { error("the loader must not run") }
    val collector = repository.listenUser().startCollecting()

    assertEquals(User(1, "Ann"), (collector.lastItem as Container.Success).value)
}
```

`listenReloadable()` is an extension over the member `listen(configuration)`
(`subjects.md` section 1), which is why the stub is written against `listen`.
The seven `any()`s are `createSubject`'s seven parameters, the last being the
loader lambda.

### Integration test: `Pending` -> `Success`

With `testSubjectFactory()` in place, `startCollecting()` alone only gets you the
*synchronous* part of the state (`Container.Pending`, from the `StateFlow`'s
initial value). The loader itself is a real suspending coroutine dispatched
on the `TestScope` the factory created, so advancing the scheduler is still
required to see it complete - `runCurrent()` is enough since nothing here
`delay()`s:

```kotlin
@Test
fun subjectEmitsPendingThenSuccess() = runFlowTest {
    val repository = UserRepository(testSubjectFactory()) { User(1, "Ann") }

    val collector = repository.listenUser().startCollecting()
    runCurrent() // let the suspending loader coroutine actually run

    assertTrue(collector.collectedItems.first() is Container.Pending)
    assertEquals(User(1, "Ann"), (collector.lastItem as Container.Success).value)
}
```

### Error path

An exception thrown by the loader becomes `Container.Error`, same as
production code (`container-type.md` section 1):

```kotlin
@Test
fun subjectEmitsErrorOnFailure() = runFlowTest {
    val expectedException = IllegalStateException("boom")
    val repository = UserRepository(testSubjectFactory()) { throw expectedException }

    val collector = repository.listenUser().startCollecting()
    runCurrent()

    val errorContainer = collector.lastItem as Container.Error
    assertEquals(expectedException, errorContainer.exception)
}
```

### Reload behaviour

`reload()` under the default `LoadConfig.Normal` resets to `Container.Pending`
before the new value arrives (`subjects.md` section 3) - so the full sequence
after one reload is **four** items, not two:

```kotlin
@Test
fun reloadProducesNewEmission() = runFlowTest {
    var callCount = 0
    val repository = UserRepository(testSubjectFactory()) {
        callCount++
        User(callCount.toLong(), "user-$callCount")
    }
    val collector = repository.listenUser().startCollecting()
    runCurrent()
    assertEquals(User(1, "user-1"), (collector.lastItem as Container.Success).value)

    repository.reload()
    runCurrent()

    assertEquals(User(2, "user-2"), (collector.lastItem as Container.Success).value)
    // Pending, Success(1), Pending again (reload resets under LoadConfig.Normal), Success(2):
    assertEquals(4, collector.count)
}
```

> This four-item count is a real thing this file's own scratch test got
> wrong on the first pass (it guessed 3, assuming reload only appends one
> item) - a concrete instance of "don't guess the emission count, run it and
> read `collectedItems`/`count` back."

### Cache-timeout behaviour

Cancelling every collector starts the `cacheTimeoutMillis` countdown
(`subjects.md` section 2, item 4-5); a new collector before it expires reuses
the cached value and does **not** re-run the loader, one after it expires
does. The timeout is a property of the factory, so it is set on
`testSubjectFactory(...)` - `UserRepository` itself is unchanged:

```kotlin
@Test
fun withinCacheTimeout_reusesCachedValue() = runFlowTest {
    var callCount = 0
    val repository = UserRepository(testSubjectFactory(cacheTimeoutMillis = 1000L)) {
        callCount++
        User(callCount.toLong(), "user-$callCount")
    }
    val collector1 = repository.listenUser().startCollecting()
    runCurrent()
    collector1.cancel()

    advanceTimeBy(999) // just under the timeout
    val collector2 = repository.listenUser().startCollecting()
    runCurrent()

    assertEquals(1, callCount)
    assertEquals(User(1, "user-1"), (collector2.lastItem as Container.Success).value)
}

@Test
fun afterCacheTimeout_startsFreshLoad() = runFlowTest {
    var callCount = 0
    val repository = UserRepository(testSubjectFactory(cacheTimeoutMillis = 1000L)) {
        callCount++
        User(callCount.toLong(), "user-$callCount")
    }
    val collector1 = repository.listenUser().startCollecting()
    runCurrent()
    collector1.cancel()

    advanceTimeBy(1001) // past the timeout
    repository.listenUser().startCollecting()
    runCurrent()

    assertEquals(2, callCount)
}
```

## 6. Testing a `LazyCache`-backed repository per argument

Same `SubjectFactory` injection, same `runCurrent()` requirement;
`listen(arg)` / `listenReloadable(arg)` keep entries independent per
argument (`subjects.md` section 4), which a test can assert directly by
starting two collectors on two different arguments:

```kotlin
private class UsersByIdRepository(
    subjectFactory: SubjectFactory,
    private val load: suspend (Long) -> User,
) {
    private val cache = subjectFactory.createCache<Long, User> { id -> emit(load(id)) }
    fun listenUser(id: Long): Flow<Container<User>> = cache.listenReloadable(id)
}
```

```kotlin
@Test
fun lazyCacheEmitsPerArgument() = runFlowTest {
    val repository = UsersByIdRepository(testSubjectFactory()) { id -> User(id, "user-$id") }

    val collector1 = repository.listenUser(1).startCollecting()
    val collector2 = repository.listenUser(2).startCollecting()
    runCurrent()

    assertEquals(User(1, "user-1"), (collector1.lastItem as Container.Success).value)
    assertEquals(User(2, "user-2"), (collector2.lastItem as Container.Success).value)
}
```

`com.elveum.container.cache.listenReloadable` (imported explicitly - it is a
same-named sibling of the subject's `listenReloadable`, see `subjects.md`'s
top note) is what makes `.listenUser(id)` return a plain `Flow`, matching the
subject case above.

## 7. Testing a reducer-based ViewModel / state holder

A `Reducer<State>` (`reducers.md` sections 1-2) needs a plain `CoroutineScope`,
and in a ViewModel that scope is `viewModelScope` - which is
`Dispatchers.Main.immediate`. **Do not add a `CoroutineScope` constructor
parameter to the ViewModel so tests can pass a test scope in.** That changes the
production shape of the class for the benefit of the test, and it is
unnecessary: the ViewModel keeps building its own scope exactly as it does in
production, and the test replaces the main dispatcher underneath it with
`kotlinx-coroutines-test`'s `Dispatchers.setMain`.

```kotlin
interface Repository {
    fun observe(): Flow<Int>
}

class CounterViewModel(repository: Repository) : ViewModel() {

    private val reducer: Reducer<Int> = repository.observe().toReducer(
        initialState = { 0 },
        nextState = { _, value -> value },
        scope = viewModelScope,
        started = SharingStarted.Lazily,
    )
    val stateFlow: StateFlow<Int> = reducer.stateFlow
}
```

```kotlin
class CounterViewModelTest {

    @MockK
    private lateinit var repository: Repository
    
    private lateinit var viewModel: CounterViewModel

    private lateinit var scope: TestScope

    private lateinit var source: MutableStateFlow<Int>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        source = MutableStateFlow(1)
        // Stub BEFORE constructing the ViewModel: `reducer` is a property
        // initializer, so `observe()` runs during construction.
        every { repository.observe() } returns source

        scope = TestScope()
        // setMain AFTER `scope` exists - it captures that scope's scheduler.
        Dispatchers.setMain(UnconfinedTestDispatcher(scope.testScheduler))

        viewModel = CounterViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reducerStateFlowReflectsSourceUpdates() = scope.runFlowTest {
        val collector = viewModel.stateFlow.startCollecting()

        assertEquals(1, collector.lastItem)
        source.value = 2
        assertEquals(2, collector.lastItem)
    }
}
```

The order inside `@Before` is load-bearing, and so are three details:

- `UnconfinedTestDispatcher(scope.testScheduler)` shares the test's own
  scheduler, so `advanceTimeBy` / `runCurrent` from `FlowTestScope` still drive
  the ViewModel's coroutines when a test needs them.
- being *unconfined*, it runs the reducer's own upstream collection eagerly and
  inline. That is why there is no `runCurrent()` between `source.value = 2` and
  the assertion, unlike section 5 where the loader runs on a
  `StandardTestDispatcher`-backed `TestScope`.
- `scope` is a field and the test body is `scope.runFlowTest { }` (section 2),
  so `@Before` and the test body share one scheduler. Plain `runFlowTest { }`
  would build a *second* `TestScope` with a different scheduler and the
  `setMain` call would be pointing at the wrong one.

Two ordering traps sit in `@Before`, and both fail confusingly:

- **Stub the mock before you construct the ViewModel.** `reducer` is a property
  initializer, so `repository.observe()` is called by the constructor. Stubbing
  it inside the test body instead leaves the constructor calling an unstubbed
  mock, and a non-relaxed `MockK` throws there rather than at the assertion.
- **Call `setMain` after `scope` is assigned.** `UnconfinedTestDispatcher(scope.testScheduler)`
  reads the scheduler eagerly, so doing it before `scope = TestScope()` either
  hits an uninitialised `lateinit` or captures a scheduler that is about to be
  replaced - and the ViewModel then runs on a scheduler no test controls.

`@After` needs only `Dispatchers.resetMain()`. Do not try to close the
ViewModel from the test: `onCleared()` is `protected` on androidx's `ViewModel`,
so it is not callable from a test class, and the scope it cancels dies with the
test process anyway.

`Dispatchers.setMain` is process-wide, so `Dispatchers.resetMain()` in `@After`
is mandatory - without it the next test class inherits a scheduler belonging to
a finished test.

`UnconfinedTestDispatcher`, `setMain` and `resetMain` are all
`@ExperimentalCoroutinesApi`; the class compiles with warnings unless it is
annotated `@OptIn(ExperimentalCoroutinesApi::class)`.

The same `setMain` setup is also the app-level answer to section 5's
`CoroutineScopeFactory` trap when a subject really is created outside any
injectable factory: `Dispatchers.Main.immediate` then resolves to the test
dispatcher. Injecting a `SubjectFactory` is still preferable, because it gives
per-test control of timeouts and decorators that a global dispatcher swap does
not.

A `ContainerReducer<State>`'s `stateFlow` (`Reducer<Container<State>>`) is
tested the same way - `startCollecting()` on it and read `Container.Pending` /
`Container.Success` / `Container.Error` off `collectedItems`, exactly as in
section 5.

## 8. Testing a `LoaderDecorator` through the injected factory

Because the injection point is the `SubjectFactory` (section 5), a
`LoaderDecorator` (`subjects.md` section 6) is swapped in the same way as the
cache timeout was - by building the factory differently, with no change to the
repository. Here a decorator that always fails the load via
`completeWithFailure`:

```kotlin
@Test
fun injectedSubjectFactoryControlsDecorator() = runFlowTest {
    val blockingDecorator = LoaderDecorator { _ ->
        completeWithFailure(IllegalStateException("blocked by decorator"))
    }
    val repository = UserRepository(testSubjectFactory(loaderDecorator = blockingDecorator)) {
        User(1, "Ann")
    }

    val collector = repository.listenUser().startCollecting()
    runCurrent()

    val error = collector.lastItem as Container.Error
    assertEquals("blocked by decorator", error.exception.message)
}
```

`cacheTimeoutMillis`, `reloadDependenciesPeriodMillis` and `loaderDecorator` are
all `DefaultSubjectFactory` constructor parameters, so one
`testSubjectFactory(...)` helper covers every one of them. Driving
`cacheTimeoutMillis` down to `0L` is a common trick: an expiry is then one
`advanceTimeBy(1)` away instead of the production default's `1000`.

If some part of the code under test calls the `SubjectFactory` **companion**
rather than an injected instance, `SubjectFactory.setFactory(...)` in `@Before`
and `SubjectFactory.resetFactory()` in `@After` swap the global instance the
same way (`subjects.md` section 5). Prefer constructor injection where you can:
the global swap is process-wide and leaks between tests if the `@After` is
forgotten.
