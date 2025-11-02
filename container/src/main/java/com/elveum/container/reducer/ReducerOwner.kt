package com.elveum.container.reducer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted

/**
 * This interface simplifies creating of new reducers by providing
 * predefined [reducerCoroutineScope] and [reducerSharingStarted] values. As a result,
 * all functions like `stateIn`, `shareIn`, `toReducer`, `toContainerReducer` can be called
 * without specifying [reducerCoroutineScope] and/or [reducerSharingStarted].
 *
 * Usage example:
 *
 * ```
 * abstract class AbstractViewModel : ViewModel(), ReducerOwner {
 *     override val reducerCoroutineScope: CoroutineScope = viewModelScope
 *     override val reducerSharingStarted: SharingStarted = SharingStarted.Lazily
 * }
 *
 * class MyViewModel(
 *     private val getReelsUseCase: GetReelsUseCase,
 * ) : AbstractViewModel() {
 *
 *     private val reducer = getReelsUseCase
 *         .invoke() // Flow<List<Reel>>
 *         .toReducer(::State)
 *     val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
 *
 *     data class State(
 *         val reels: List<Reel>,
 *     )
 * }
 * ```
 */
public interface ReducerOwner {
    public val reducerCoroutineScope: CoroutineScope
    public val reducerSharingStarted: SharingStarted
}
