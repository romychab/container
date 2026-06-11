package com.elveum.store.demo.feature.examples.store_simple.combined

import com.elveum.store.demo.feature.examples.store_simple.combined.GalleryRepository.GalleryImage
import kotlinx.coroutines.delay
import javax.inject.Inject

class LocalGalleryDataSource @Inject constructor() {

    private var savedImages: MutableList<GalleryImage>? = null

    suspend fun loadImages(query: String): List<GalleryImage>? {
        delay(200)
        return savedImages?.filter { query.isBlank() || it.name.contains(query) }
    }

    suspend fun saveImages(query: String, images: List<GalleryImage>) {
        delay(200)
        this.savedImages = images
            .map { it.copy(isLocal = true) }
            .toMutableList()
    }

    suspend fun updateImage(image: GalleryImage) {
        delay(200)
        val index = savedImages?.indexOfFirst { it.id == image.id }?.takeIf { it != -1 }
        if (index != null) {
            savedImages?.set(index, image.copy(isLocal = true))
        }
    }

}