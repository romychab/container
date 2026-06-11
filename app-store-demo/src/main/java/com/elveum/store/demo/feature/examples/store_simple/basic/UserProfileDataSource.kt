package com.elveum.store.demo.feature.examples.store_simple.basic

import com.elveum.store.demo.errors.ErrorFlagProvider
import com.elveum.store.demo.feature.examples.store_simple.basic.UserProfileRepository.UserProfile
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class UserProfileDataSource @Inject constructor(
    private val errorFlagProvider: ErrorFlagProvider,
    private val faker: Faker,
) {

    private var profile = UserProfile(
        name = faker.name().fullName(),
        address = faker.address().fullAddress(),
        shortBio = faker.lorem().sentence(10),
        age = 32,
    )

    suspend fun fetchUserProfile(): UserProfile {
        delay(2000)
        if (errorFlagProvider.isErrorFlagEnabled()) throw RuntimeException("Failed to fetch the user profile.")
        return profile
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        delay(1000)
        if (errorFlagProvider.isErrorFlagEnabled()) throw RuntimeException("Failed to update user profile.")
        this.profile = profile
    }

}