package com.elveum.store.demo.di

import com.github.javafaker.Faker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.Random as JavaRandom
import kotlin.random.Random as KotlinRandom

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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
