package com.elveum.store.demo.feature.examples.store_paged.pagination_basic

import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PhotoRepository @Inject constructor(
    private val dataSource: PhotoDataSource,
) {

    private val store = StoreFactory.pagedStoreBuilder<Int, Photo>(
        initialKey = 0,
        itemId = Photo::id,
    ).build(onFetch = dataSource::fetchPage)

    fun getPhotos(): Flow<StoreResult<List<Photo>>> = store.observe()

    fun onItemRendered(index: Int) {
        store.onItemRendered(index)
    }

    data class Photo(
        val id: Int,
        val title: String,
        val description: String,
        val url: String,
    )

}
