package com.elveum.store.demo.feature.examples.store_paged.pagination_args

import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PhotoRepository @Inject constructor(
    private val dataSource: PhotoDataSource,
) {

    private val store = StoreFactory.pagedStoreBuilder<Int, Photo>(initialKey = 0, Photo::id)
        .withQuery<Set<PhotoCategory>>(PhotoCategory.entries.toSet())
        .setLoadRequest(LoadRequest.Silent)
        .build(
            onFetch = dataSource::fetchPage,
        )

    fun getPhotos(): Flow<StoreResult<List<Photo>>> = store.observe()

    fun getSelectedCategories(): StateFlow<Set<PhotoCategory>> = store.queryFlow

    fun toggleCategory(category: PhotoCategory) {
        val currentFilter = store.queryFlow.value
        val newFilter = if (currentFilter.contains(category)) {
            currentFilter - category
        } else {
            currentFilter + category
        }
        store.submitQueryAsync(newFilter)
    }

    data class Photo(
        val id: Int,
        val title: String,
        val description: String,
        val url: String,
        val category: PhotoCategory,
    )

    enum class PhotoCategory {
        Nature,
        City,
        Art,
    }

}
