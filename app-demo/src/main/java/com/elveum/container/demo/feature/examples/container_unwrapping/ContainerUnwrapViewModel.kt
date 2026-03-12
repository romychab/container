@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.demo.feature.examples.container_unwrapping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.pendingContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ContainerUnwrapViewModel @Inject constructor(
    private val repository: MatrixRepository,
) : ViewModel() {

    val stateFlow = flow { emit(repository.fetchMatrixEffect()) }
        .stateIn(
            scope = viewModelScope,
            started = WhileSubscribed(),
            initialValue = pendingContainer(),
        )

}
