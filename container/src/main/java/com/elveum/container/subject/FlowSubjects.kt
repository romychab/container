package com.elveum.container.subject

import com.elveum.container.subject.factories.FlowSubjectFactory
import com.elveum.container.subject.factories.LazyFlowSubjectFactory
import kotlinx.coroutines.CoroutineDispatcher

public object FlowSubjects {

    internal var defaultConfiguration = Configuration()

    /**
     * Change the default configuration of creating subjects by your
     * own configuration.
     * @see Configuration
     */
    public fun setDefaultConfiguration(configuration: Configuration) {
        this.defaultConfiguration = configuration
    }

    /**
     * Reset the configuration of creating subject to the default implementation.
     */
    public fun resetDefaultConfiguration() {
        this.defaultConfiguration = Configuration()
    }

    private class FlowSubjectFactoryImpl : FlowSubjectFactory {
        override fun <T> create(): FlowSubject<T> {
            return createDefaultFlowSubject()
        }
    }

    private class LazyFlowSubjectFactoryImpl : LazyFlowSubjectFactory {
        override fun <T> create(
            cacheTimeoutMillis: Long,
            loadingDispatcher: CoroutineDispatcher,
            creator: Creator,
        ): LazyFlowSubject<T> {
            return creator.createDefault(loadingDispatcher, cacheTimeoutMillis)
        }
    }

    public class Configuration(

        /**
         * Defines how a [FlowSubject] instances will be created when
         * calling [FlowSubject.create] method.
         */
        internal val flowSubjectFactory: FlowSubjectFactory = FlowSubjectFactoryImpl(),

        /**
         * Defined how a [LazyFlowSubject] instances will be created when
         * calling [LazyFlowSubject.create] method.
         */
        internal val lazyFlowSubjectFactory: LazyFlowSubjectFactory = LazyFlowSubjectFactoryImpl()

    )
}