package com.elveum.store.internal.builders.paged

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.internal.builders.BaseBuilderImpl

internal class BasePageBuilderImpl<P : Any, T : Any, OutBuilder : Any>(
    val pageConfig: SharedPageConfig<P, T>,
) : BaseBuilderImpl<OutBuilder>(pageConfig), BasePagedBuilder<OutBuilder> {

    override fun setFetchDistance(itemCount: Int): OutBuilder {
        pageConfig.fetchDistance = itemCount
        return ref
    }

}
