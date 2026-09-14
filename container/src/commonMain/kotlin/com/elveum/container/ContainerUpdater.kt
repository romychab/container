package com.elveum.container

public interface ContainerUpdater {
    public var metadata: ContainerMetadata

    public var reloadFunction: ReloadFunction
    public var backgroundLoadState: BackgroundLoadState
    public var sourceType: SourceType
}

internal class ContainerUpdaterImpl(
    origin: ContainerMapperScope,
) : ContainerUpdater {

    override var metadata: ContainerMetadata = origin.metadata

    override var reloadFunction: ReloadFunction
        get() = metadata.reloadFunction
        set(value) {
            metadata += ReloadFunctionMetadata(value)
        }

    override var backgroundLoadState: BackgroundLoadState
        get() = metadata.backgroundLoadState
        set(value) {
            metadata += BackgroundLoadMetadata(value)
        }

    override var sourceType: SourceType
        get() = metadata.sourceType
        set(value) {
            metadata += SourceTypeMetadata(value)
        }
}
