package com.elveum.container

public interface ContainerUpdater : ContainerMapperScope {
    public override var reloadFunction: ReloadFunction
    public override var isLoadingInBackground: Boolean
    public override var source: SourceType
}

internal class ContainerUpdaterImpl(
    origin: ContainerMapperScope,
) : ContainerUpdater, ContainerMapperScope {
    override var reloadFunction: ReloadFunction = origin.reloadFunction
    override var isLoadingInBackground: Boolean = origin.isLoadingInBackground
    override var source: SourceType = origin.source
}
