package com.elveum.container.demo.di

import com.elveum.container.factory.DefaultSubjectFactory
import com.elveum.container.factory.SubjectFactory
import com.github.javafaker.Faker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Random
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSubjectFactory(): SubjectFactory {
        return DefaultSubjectFactory(cacheTimeoutMillis = 30_000L)
    }

    @Provides
    fun provideFaker(): Faker {
        return Faker.instance(Random(0))
    }

}
