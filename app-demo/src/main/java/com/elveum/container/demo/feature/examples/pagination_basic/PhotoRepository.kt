package com.elveum.container.demo.feature.examples.pagination_basic

import com.elveum.container.Container
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.paging.pageLoader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PhotoRepository @Inject constructor(
    private val dataSource: PhotoDataSource,
) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int?, Photo>(
            initialKey = null,
            itemId = Photo::id,
        ) { pageKey ->
            val result = dataSource.fetchPage(pageKey)
            emitPage(result.photos)
            if (result.nextPageKey != null) emitNextKey(result.nextPageKey)
        }
    )

    fun getPhotos(): Flow<Container<List<Photo>>> = subject.listenReloadable()


    data class Photo(
        val id: Int,
        val title: String,
        val description: String,
        val url: String,
    )

    data class PagedResult(
        val photos: List<Photo>,
        val nextPageKey: Int?,
    )

}
