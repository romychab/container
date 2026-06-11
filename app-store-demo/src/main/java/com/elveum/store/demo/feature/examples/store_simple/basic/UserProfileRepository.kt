package com.elveum.store.demo.feature.examples.store_simple.basic

import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.update
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserProfileRepository @Inject constructor(
    private val dataSource: UserProfileDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<UserProfile>()
        .build(onFetch = dataSource::fetchUserProfile)

    fun getUserProfile(): Flow<StoreResult<UserProfile>> {
        return store.observe()
    }

    fun reload() {
        store.invalidateAsync()
    }

    suspend fun update(newProfile: UserProfile) {
        store.optimisticUpdate {
            emit(newProfile)
            dataSource.updateUserProfile(newProfile)
        }
    }

    data class UserProfile(
        val name: String,
        val address: String,
        val shortBio: String,
        val age: Int,
    )

}
