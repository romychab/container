package com.elveum.container

public class LoadNotFinishedException() : Exception(
    "Container.Pending can't be unwrapped since its value is still being loaded.",
)
