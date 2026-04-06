package com.elveum.container.demo.feature.examples.subject_basics

import com.elveum.container.Container
import com.elveum.container.demo.errors.ErrorFlagProvider
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserProfileRepository @Inject constructor(
    private val errorFlagProvider: ErrorFlagProvider,
    private val faker: Faker,
) {

    private val subject = LazyFlowSubject.create {
        delay(2000)
        if (errorFlagProvider.isErrorFlagEnabled()) throw RuntimeException("Failed to fetch the user profile.")
        val userProfile = UserProfile(
            name = faker.name().fullName(),
            address = faker.address().fullAddress(),
            shortBio = faker.lorem().sentence(10),
            age = 32,
        )
        emit(userProfile)
    }

    fun getUserProfile(): Flow<Container<UserProfile>> {
        return subject.listenReloadable()
    }

    data class UserProfile(
        val name: String,
        val address: String,
        val shortBio: String,
        val age: Int,
    )

}
