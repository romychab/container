package com.elveum.container.reducer.owner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted

/**
 * This interface simplifies creating of new reducers by providing
 * predefined [reducerScope] and [sharingStarted] values. As a result,
 * all functions like `combineToReducer`, `combineContainersToReducer`,
 * `toReducer` can be called without specifying [reducerScope] and/or
 * [sharingStarted].
 *
 * Usage example:
 *
 * ```
 * abstract class AbstractViewModel : ViewModel(), ReducerOwner {
 *     override val reducerScope: CoroutineScope = viewModelScope
 *     override val sharingStarted: SharingStarted = SharingStarted.Lazily
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

    public val reducerScope: CoroutineScope

    public val sharingStarted: SharingStarted

}
