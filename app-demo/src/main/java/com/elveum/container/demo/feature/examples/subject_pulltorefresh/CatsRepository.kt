package com.elveum.container.demo.feature.examples.subject_pulltorefresh

import com.elveum.container.ListContainerFlow
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatsRepository @Inject constructor(
    private val faker: Faker,
) {

    private val subject = LazyFlowSubject.create {
        val cats = loadCats()
        emit(cats)
    }

    fun getCats(): ListContainerFlow<Cat> = subject.listenReloadable()

    private suspend fun loadCats(): List<Cat> {
        delay(2000)
        return List(20) {
            Cat(
                id = it + 1L,
                name = faker.cat().name(),
                description = faker.lorem().sentence(5, 5),
                imageUrl = CatImages[it % CatImages.size]
            )
        }
    }

    data class Cat(
        val id: Long,
        val name: String,
        val imageUrl: String,
        val description: String,
    )
}


private val CatImages = listOf(
    "https://images.unsplash.com/photo-1615454299901-de13b71ecaae?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODAyNw&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1604916287784-c324202b3205?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODAzMQ&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1596854372407-baba7fef6e51?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODAzOA&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1608032364895-0da67af36cd2?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODA0Mw&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1571988840298-3b5301d5109b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODA0Ng&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1494256997604-768d1f608cac?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODEyMQ&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/flagged/photo-1557427161-4701a0fa2f42?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODEyNQ&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1640384974326-3e72680e0fb3?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODI0MA&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1623876159473-5e79be88f7ac?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODIzMw&ixlib=rb-1.2.1&q=80&w=1080"
)
