package com.elveum.container

public interface ContainerUpdater : ContainerMapperScope {

    public override var metadata: ContainerMetadata

    public override var reloadFunction: ReloadFunction
        get() = super.reloadFunction
        set(value) {
            metadata += ReloadFunctionMetadata(value)
        }

    public override var isLoadingInBackground: Boolean
        get() = super.isLoadingInBackground
        set(value) {
            metadata += IsLoadingInBackgroundMetadata(value)
        }

    public override var source: SourceType
        get() = super.source
        set(value) {
            metadata += SourceTypeMetadata(value)
        }
}

internal class ContainerUpdaterImpl(
    origin: ContainerMapperScope,
) : ContainerUpdater, ContainerMapperScope {

    override var metadata: ContainerMetadata = origin.metadata

}
