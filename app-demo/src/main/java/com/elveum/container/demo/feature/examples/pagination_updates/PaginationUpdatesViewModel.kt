package com.elveum.container.demo.feature.examples.pagination_updates

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.pagination_updates.ProductRepository.Product
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.reducer.containerToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaginationUpdatesViewModel @Inject constructor(
    private val repository: ProductRepository,
) : AbstractViewModel() {

    private val reducer = repository
        .getProducts()
        .containerToReducer(
            initialState = ::StateImpl,
            nextState = StateImpl::copy,
        )

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    fun toggleLike(product: UiProduct) {
        viewModelScope.launch {
            try {
                setTogglingState(product, true)
                repository.toggleLike(product.origin)
            } finally {
                setTogglingState(product, false)
            }
        }
    }

    private fun setTogglingState(product: UiProduct, value: Boolean) {
        reducer.updateState { oldState ->
            val oldTogglingSet = oldState.togglingSet
            val newTogglingSet = if (value) {
                oldTogglingSet + product.id
            } else {
                oldTogglingSet - product.id
            }
            oldState.copy(togglingSet = newTogglingSet)
        }
    }

    @Immutable
    data class UiProduct(
        val origin: Product,
        val isToggling: Boolean,
    ) : Product by origin

    @Immutable
    interface State {
        val products: ImmutableList<UiProduct>
    }

    private data class StateImpl(
        val originProducts: List<Product>,
        val togglingSet: Set<Int> = emptySet(),
    ) : State {

        override val products = originProducts
            .map {
                UiProduct(
                    origin = it,
                    isToggling = togglingSet.contains(it.id),
                )
            }
            .toImmutableList()
    }

}
