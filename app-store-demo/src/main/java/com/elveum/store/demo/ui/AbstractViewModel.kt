package com.elveum.store.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.store.demo.effects.Toaster
import com.elveum.container.reducer.ReducerOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

abstract class AbstractViewModel : ViewModel(), ReducerOwner {

    override val reducerCoroutineScope: CoroutineScope = viewModelScope
    override val reducerSharingStarted: SharingStarted = SharingStarted.Lazily

    fun safeLaunch(
        toaster: Toaster,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toaster.toast(e.message)
            }
        }
    }
}
