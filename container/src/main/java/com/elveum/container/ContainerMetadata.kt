package com.elveum.container

/**
 * Any container can have additional attached metadata represented
 * by instances of this interface.
 */
public interface ContainerMetadata {

    /**
     * Combine metadata instances. The 'plus' operator can be used
     * for replacing existing metadata as well if [other] is an instance of type
     * that already exists in metadata.
     */
    public operator fun plus(other: ContainerMetadata?): ContainerMetadata {
        if (other == null) return this
        val metadataList = if (this is CombinedMetadata && other is CombinedMetadata) {
            this.metadataList + other.metadataList
        } else if (this is CombinedMetadata) {
            this.metadataList + other
        } else if (other is CombinedMetadata) {
            listOf(this) + other.metadataList
        } else {
            listOf(this, other)
        }
        return metadataList.merge()
    }

    /**
     * Filter metadata entries by the specified [predicate].
     */
    public fun filter(predicate: (ContainerMetadata) -> Boolean): ContainerMetadata {
        return if (predicate(this)) {
            this
        } else {
            EmptyMetadata
        }
    }

}

/**
 * Get the metadata instance of type [T].
 */
public inline fun <reified T : ContainerMetadata> ContainerMetadata.get(): T? {
    return if (this is T) {
        this
    } else if (this is CombinedMetadata) {
        this.metadataList.firstOrNull { it is T } as? T
    } else {
        null
    }
}

/**
 * Create metadata instance initialized with default fields: [source], [isLoadingInBackground],
 * and [reloadFunction].
 */
public fun defaultMetadata(
    source: SourceType? = null,
    isLoadingInBackground: Boolean? = null,
    reloadFunction: ReloadFunction? = null,
): ContainerMetadata {
    return EmptyMetadata +
            source?.let(::SourceTypeMetadata) +
            isLoadingInBackground?.let(::IsLoadingInBackgroundMetadata) +
            reloadFunction?.let(::ReloadFunctionMetadata)
}

public val ContainerMetadata.isLoadingInBackground: Boolean
    get() = get<IsLoadingInBackgroundMetadata>()?.isLoading ?: false

public val ContainerMetadata.reloadFunction: ReloadFunction
    get() = get<ReloadFunctionMetadata>()?.reloadFunction ?: EmptyReloadFunction

public val ContainerMetadata.sourceType: SourceType
    get() = get<SourceTypeMetadata>()?.sourceType ?: UnknownSourceType

public data class IsLoadingInBackgroundMetadata(
    public val isLoading: Boolean
) : ContainerMetadata

public data class ReloadFunctionMetadata(
    public val reloadFunction: (silently: Boolean) -> Unit,
) : ContainerMetadata

public data class SourceTypeMetadata(
    public val sourceType: SourceType,
) : ContainerMetadata

public data class LoadUuidMetadata(
    public val uuid: String,
) : ContainerMetadata

public data object EmptyMetadata : ContainerMetadata {
    override fun plus(other: ContainerMetadata?): ContainerMetadata {
        return other ?: EmptyMetadata
    }
}

@PublishedApi
internal class CombinedMetadata(
    val metadataList: List<ContainerMetadata>,
) : ContainerMetadata {

    val sortedByTypeMetadata by lazy {
        metadataList.sortedBy { it::class.qualifiedName ?: "" }
    }

    override fun filter(predicate: (ContainerMetadata) -> Boolean): ContainerMetadata {
        return metadataList.filter(predicate).merge()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CombinedMetadata
        return sortedByTypeMetadata == other.sortedByTypeMetadata
    }

    override fun hashCode(): Int {
        return sortedByTypeMetadata.hashCode()
    }

    override fun toString(): String {
        return "CombinedMetadata$sortedByTypeMetadata"
    }

}

private fun List<ContainerMetadata>.merge(): ContainerMetadata {
    val mergedList = asReversed()
        .distinctBy { it::class }
        .filterNot { it is EmptyMetadata }
        .asReversed()
    return if (mergedList.isEmpty()) {
        EmptyMetadata
    } else if (mergedList.size == 1) {
        mergedList.single()
    } else {
        CombinedMetadata(mergedList)
    }
}
