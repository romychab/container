package com.elveum.container.demo.feature.examples.subject_errors

import com.elveum.container.ListContainerFlow
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    private val faker: Faker,
) {

    private var isError = false

    private val subject = LazyFlowSubject.create {
        delay(2000)
        isError = !isError
        if (isError) throw RuntimeException("Failed to load images.")
        val cats = loadImages()
        emit(cats)
    }

    fun getGallery(): ListContainerFlow<GalleryImage> = subject.listenReloadable()

    private fun loadImages(): List<GalleryImage> {
        return List(20) {
            GalleryImage(
                id = it + 1L,
                name = faker.pokemon().name(),
                imageUrl = Images[it % Images.size]
            )
        }
    }

    data class GalleryImage(
        val id: Long,
        val name: String,
        val imageUrl: String,
    )
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
