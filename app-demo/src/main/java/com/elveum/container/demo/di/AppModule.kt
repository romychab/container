package com.elveum.container.demo.di

import com.elveum.container.factory.DefaultSubjectFactory
import com.elveum.container.factory.SubjectFactory
import com.github.javafaker.Faker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.Random as JavaRandom
import kotlin.random.Random as KotlinRandom
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
        return Faker.instance(JavaRandom(0))
    }

    @Provides
    fun provideKotlinRandom(): KotlinRandom {
        return KotlinRandom(0)
    }

    @Provides
    @StateProducerDispatcher
    fun provideStateProducerDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

}
