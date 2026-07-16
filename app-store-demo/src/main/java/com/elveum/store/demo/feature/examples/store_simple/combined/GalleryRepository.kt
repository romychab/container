package com.elveum.store.demo.feature.examples.store_simple.combined

import com.elveum.store.StoreFactory
import com.elveum.store.demo.errors.ErrorFlagProvider
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    private val localSource: LocalGalleryDataSource,
    private val remoteSource: RemoteGalleryDataSource,
    errorFlagProvider: ErrorFlagProvider,
) {

    private val loadRequestFlow = errorFlagProvider.isKeepContentOnErrorFlagEnabled()
        .map {isKeepContentOnError ->
            LoadRequest.builder().run {
                if (isKeepContentOnError) {
                    keepContentOnLoadAndError().keepContentOnQueryAndError()
                } else {
                    keepContentOnLoad().keepContentOnQuery()
                }
            }.build()
        }

    private val store = StoreFactory.simpleStoreBuilder<List<GalleryImage>>()
        .addSuspendingLocalStorage()
        .withQuery(initialQuery = "", debounceMillis = 500)
        .setLoadRequest(loadRequestFlow)
        .build(
            onFetch = remoteSource::fetchImages,
            onSaveToStorage = localSource::saveImages,
            onLoadFromStorage = localSource::loadImages,
        )

    fun getGallery(): Flow<StoreResult<List<GalleryImage>>> = store.observe()

    fun getQuery(): Flow<String> = store.queryFlow

    suspend fun toggleLike(image: GalleryImage) {
        store.optimisticUpdate { oldList ->
            val updatedImage = image.copy(isLiked = !image.isLiked)
            val optimisticList = oldList.map { if (it.id == updatedImage.id) updatedImage else it }
            emit(optimisticList) // immediately display optimistic value
            remoteSource.updateImage(updatedImage) // update on remote
            localSource.updateImage(updatedImage) // if remote update is success -> update local store
        }
    }

    suspend fun setQuery(query: String) {
        store.submitQuery(query)
    }

    data class GalleryImage(
        val id: Long,
        val name: String,
        val imageUrl: String,
        val isLiked: Boolean,
        val isLocal: Boolean,
    )
}

