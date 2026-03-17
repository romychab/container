package com.elveum.container.demo.feature.examples.subject_chunks

import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.demo.feature.examples.subject_chunks.MosaicRepository.MosaicData
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChunksViewModel @Inject constructor(
    repository: MosaicRepository,
) : AbstractViewModel() {

    val stateFlow: StateFlow<Container<MosaicData>> = repository
        .getMosaicData()
        .stateIn(pendingContainer())

}
