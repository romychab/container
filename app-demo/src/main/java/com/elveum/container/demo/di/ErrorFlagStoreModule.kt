package com.elveum.container.demo.di

import com.elveum.container.demo.errors.ErrorFlagProvider
import com.elveum.container.demo.errors.ErrorFlagRepository
import com.elveum.container.demo.errors.ErrorFlagStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ErrorFlagStoreModule {

    @Binds
    fun bindToRepository(impl: ErrorFlagStore): ErrorFlagRepository

    @Binds
    fun bindToProvider(impl: ErrorFlagStore): ErrorFlagProvider

}
