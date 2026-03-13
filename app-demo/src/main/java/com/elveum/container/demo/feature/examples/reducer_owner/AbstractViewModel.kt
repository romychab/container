package com.elveum.container.demo.feature.examples.reducer_owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.reducer.ReducerOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted

abstract class AbstractViewModel : ViewModel(), ReducerOwner {

    override val reducerCoroutineScope: CoroutineScope = viewModelScope
    override val reducerSharingStarted: SharingStarted = SharingStarted.WhileSubscribed(
        stopTimeoutMillis = 1000,
        replayExpirationMillis = 1000,
    )

}
