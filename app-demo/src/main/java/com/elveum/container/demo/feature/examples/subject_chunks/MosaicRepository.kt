package com.elveum.container.demo.feature.examples.subject_chunks

import androidx.compose.ui.graphics.Color
import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.subject_chunks.MosaicDataSource.MosaicInfo
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class MosaicRepository @Inject constructor(
    private val dataSource: MosaicDataSource,
) {

    private val subject = LazyFlowSubject.create {
        // Fetch basic mosaic data
        val info = dataSource.fetchMosaicInfo()
        val tiles = arrayOfNulls<Color>(info.rows * info.cols)
        emit(MosaicData(info, tiles.toList()))

        // Fetch all tiles in parallel, emitting state after each arrival
        val mutex = Mutex()
        val emitter = this
        coroutineScope {
            for (row in 0 until info.rows) {
                for (col in 0 until info.cols) {
                    launch {
                        val color = dataSource.fetchTile(info.designIndex, row, col)
                        mutex.withLock {
                            tiles[row * info.cols + col] = color
                            emitter.emit(MosaicData(info, tiles.toList()))
                        }
                    }
                }
            }
        }
    }

    fun getMosaicData(): Flow<Container<MosaicData>> = subject.listenReloadable()

    data class MosaicData(
        val info: MosaicInfo,
        val tiles: List<Color?>,
    ) {
        val loadedCount: Int = tiles.count { it != null }
        val totalTiles: Int = info.rows * info.cols

        fun getTile(row: Int, col: Int): Color? = tiles[row * info.cols + col]
    }
}
