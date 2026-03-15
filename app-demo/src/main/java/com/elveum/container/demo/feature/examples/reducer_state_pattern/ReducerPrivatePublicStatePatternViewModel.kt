package com.elveum.container.demo.feature.examples.reducer_state_pattern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.Container
import com.elveum.container.demo.di.StateProducerDispatcher
import com.elveum.container.demo.feature.examples.reducer_state_pattern.TreeRepository.Node
import com.elveum.container.demo.feature.examples.reducer_state_pattern.TreeRepository.Tree
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.containerToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus
import javax.inject.Inject

@HiltViewModel
class ReducerPrivatePublicStatePatternViewModel @Inject constructor(
    private val repository: TreeRepository,
    @StateProducerDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val reducer: ContainerReducer<StateImpl> = repository
        .getTree() // Flow<Container<Tree>>
        .containerToReducer(
            initialState = ::StateImpl,
            nextState = StateImpl::copy,
            scope = viewModelScope + dispatcher,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 1000,
                replayExpirationMillis = 1000,
            )
        )

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    fun toggle(item: FlattenedItem) {
        // update private state if needed:
        reducer.updateState { oldState ->
            val newExpandedNodes = if (item.isExpanded) {
                oldState.expandedNodes - item.nodeId
            } else {
                oldState.expandedNodes + item.nodeId
            }
            oldState.copy(expandedNodes = newExpandedNodes)
        }
    }

    fun delete(item: FlattenedItem) {
        repository.deleteNode(item.nodeId)
    }

    // Private state that can be updated by the ViewModel:
    private data class StateImpl(
        // items from data layer:
        val tree: Tree,
        // additional data managed by view-model:
        val expandedNodes: Set<Long> = setOf(tree.rootNode.id),
    ) : State { // <-- private state implements public state

        override val flattenedItems: List<FlattenedItem> = tree.rootNode.flatten()

        private fun Node.flatten(level: Int = 0): List<FlattenedItem> {
            val isExpanded = expandedNodes.contains(id)
            val item = FlattenedItem(
                nodeId = id,
                title = title,
                description = description,
                isExpanded = isExpanded,
                hasChildren = children.isNotEmpty(),
                level = level,
            )
            return if (isExpanded) {
                listOf(item) + children.flatMap { child -> child.flatten(level + 1) }
            } else {
                listOf(item)
            }
        }
    }

    data class FlattenedItem(
        val nodeId: Long,
        val title: String,
        val description: String,
        val isExpanded: Boolean,
        val hasChildren: Boolean,
        val level: Int,
    )

    // Public state emitted to Screen:
    interface State {
        val flattenedItems: List<FlattenedItem>
    }

}
