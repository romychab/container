package com.elveum.container.demo.feature.examples.container_of

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.Container
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContainerOfViewModel @Inject constructor(
    private val repository: VectorGraphicRepository,
) : ViewModel() {

    private val _stateFlow = MutableStateFlow<Container<VectorGraphicRepository.VectorGraphic>>(
        successContainer(repository.initialGraphic())
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _stateFlow.value = pendingContainer()
            _stateFlow.value = repository.loadGraphic()
        }
    }
}
