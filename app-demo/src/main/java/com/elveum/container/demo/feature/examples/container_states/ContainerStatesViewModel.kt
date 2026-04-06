package com.elveum.container.demo.feature.examples.container_states

import androidx.lifecycle.ViewModel
import com.elveum.container.Container
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ContainerStatesViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<Container<String>>(pendingContainer())
    val state: StateFlow<Container<String>> = _state

    fun showLoading() {
        _state.update {
            pendingContainer()
        }
    }

    fun showError() {
        _state.update {
            errorContainer(RuntimeException("This is an example error message."))
        }
    }

    fun showSuccess() {
        _state.update {
            successContainer("Hello from the Container library!")
        }
    }

}
