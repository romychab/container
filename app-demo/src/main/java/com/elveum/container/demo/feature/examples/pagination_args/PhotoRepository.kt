package com.elveum.container.demo.feature.examples.pagination_args

import com.elveum.container.Container
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.paging.pageLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class PhotoRepository @Inject constructor(
    private val dataSource: PhotoDataSource,
) {

    private val selectedCategories = MutableStateFlow(PhotoCategory.entries.toSet())

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int?, Photo>(
            initialKey = null,
            itemId = Photo::id,
        ) { pageKey ->
            val categories = dependsOnFlow("categoryFilter") { selectedCategories }
            val result = dataSource.fetchPage(pageKey, categories)
            emitPage(result.photos)
            if (result.nextPageKey != null) emitNextKey(result.nextPageKey)
        }
    )

    fun getPhotos(): Flow<Container<List<Photo>>> = subject.listenReloadable()

    fun getSelectedCategories(): StateFlow<Set<PhotoCategory>> = selectedCategories

    fun toggleCategory(category: PhotoCategory) {
        selectedCategories.update {
            if (it.contains(category)) it - category else it + category
        }
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

    data class PagedResult(
        val photos: List<Photo>,
        val nextPageKey: Int?,
    )

}
