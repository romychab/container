package com.elveum.store.demo.feature.examples.store_simple.combined

import com.elveum.store.demo.errors.ErrorFlagProvider
import com.elveum.store.demo.feature.examples.store_simple.combined.GalleryRepository.GalleryImage
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class RemoteGalleryDataSource @Inject constructor(
    private val faker: Faker,
    private val errorFlagProvider: ErrorFlagProvider,
) {

    private val images = MutableList(20) {
        GalleryImage(
            id = it + 1L,
            name = faker.pokemon().name(),
            imageUrl = Images[it % Images.size],
            isLiked = false,
            isLocal = false,
        )
    }

    suspend fun fetchImages(query: String): List<GalleryImage> {
        delay(3000)
        if (errorFlagProvider.isErrorFlagEnabled()) {
            throw RuntimeException("Failed to fetch images.")
        }
        return images.filter { query.isBlank() || it.name.contains(query) }
    }

    suspend fun updateImage(image: GalleryImage) {
        delay(1500)
        if (errorFlagProvider.isErrorFlagEnabled()) {
            throw RuntimeException("Failed to update the image.")
        }
        images.indexOfFirst { it.id == image.id }
            .takeIf { it != -1 }
            ?.let { index -> images[index] = image }
    }
}


private val Images = listOf(
    "https://fastly.picsum.photos/id/736/640/480.jpg?hmac=JnUBykhF4XAlrtn8JfYc6B5QJgzXKWVYW7prxNdJvuU",
    "https://fastly.picsum.photos/id/480/640/480.jpg?hmac=5w66WGqSyWEV7-XqRn5mZCY-XN0MGIkQWcP7okfP7nI",
    "https://fastly.picsum.photos/id/288/640/480.jpg?hmac=NgFZuskFw2sztMrWfh0w-4mD_SpyD1uEJ_wrwzWRpKE",
    "https://fastly.picsum.photos/id/360/640/480.jpg?hmac=WD3VHrBFIEfK1qwJSNAldRd2Fazt1n1yzBzI46nAkHk",
    "https://fastly.picsum.photos/id/451/640/480.jpg?hmac=i_Y-Lc0vCzuWsoo33B0GaZMLgPFB5WVUSL8v9E3G7N8",
    "https://fastly.picsum.photos/id/251/640/480.jpg?hmac=cOtvJK-b2IFrpeMcLuyenlXHzWvtjR_5cahrUQSs6f0",
    "https://fastly.picsum.photos/id/229/640/480.jpg?hmac=HXwgmcEJ8QfIrrVkO36VjFYpWvJeo7jIW25QnczyuDI",
    "https://fastly.picsum.photos/id/991/640/480.jpg?hmac=R6eKFpX4zgCHv9ezShnrBh0cQZpnb_CRp3ZemH_8YmE",
)
