package com.elveum.container

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combine two StateFlows containing [Container] instances into one.
 */
public fun <T1, T2, R> combineContainerFlows(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    transform: (T1, T2) -> R,
): Flow<Container<R>> {
    return combineContainerFlows(listOf(flow1, flow2)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2)
    }
}

/**
 * Combine three StateFlows containing [Container] instances into one.
 */
public fun <T1, T2, T3, R> combineContainerFlows(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    flow3: Flow<Container<T3>>,
    transform: (T1, T2, T3) -> R,
): Flow<Container<R>> {
    return combineContainerFlows(listOf(flow1, flow2, flow3)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2, input[2] as T3)
    }
}

/**
 * Combine four StateFlows containing [Container] instances into one.
 */
public fun <T1, T2, T3, T4, R> combineContainerFlows(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    flow3: Flow<Container<T3>>,
    flow4: Flow<Container<T4>>,
    transform: (T1, T2, T3, T4) -> R,
): Flow<Container<R>> {
    return combineContainerFlows(listOf(flow1, flow2, flow3, flow4)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2, input[2] as T3, input[3] as T4)
    }
}

/**
 * Combine five StateFlows containing [Container] instances into one.
 */
public fun <T1, T2, T3, T4, T5, R> combineContainerFlows(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    flow3: Flow<Container<T3>>,
    flow4: Flow<Container<T4>>,
    flow5: Flow<Container<T5>>,
    transform: (T1, T2, T3, T4, T5) -> R,
): Flow<Container<R>> {
    return combineContainerFlows(listOf(flow1, flow2, flow3, flow4, flow5)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2, input[2] as T3, input[3] as T4, input[4] as T5)
    }
}

/**
 * Combine multiple StateFlows containing [Container] instances into one.
 *
 * Transformation logic:
 * - Transform function is executed only if all origin flows contain [Container.Success]
 *   values. The resulting container will be [Container.Success] if the transformation
 *   function is executed successfully.
 * - Otherwise, if the transformation function fails, the resulting container will be [Container.Error].
 * - If at least one origin flow contains [Container.Error], the result will be [Container.Error] as well.
 * - By default, the resulting source value is taken from the first flow.
 * - By default, the resulting reload function executes all origin reload functions from all flows.
 * - By default, the resulting isLoading value is `true` if at least one isLoading value from
 *   any origin flow is `true`.
 */
public fun <R> combineContainerFlows(
    flows: Iterable<Flow<Container<*>>>,
    transform: suspend CombineContainerFlowScope.(List<*>) -> R,
): Flow<Container<R>> {
    return combine(flows) { containers ->
        val scope = CombineContainerFlowScopeImpl(containers)
        val container = if (containers.all { it is Container.Success<*> }) {
            val values = containers.map { (it as Container.Success<*>).value }
            try {
                val transformedValue = transform(scope, values)
                successContainer(transformedValue)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorContainer(e)
            }
        } else {
            val errorContainer = containers
                .firstOrNull { it is Container.Error } as? Container.Error
            errorContainer ?: Container.Pending
        }
        container.update {
            sourceType = scope.sourceType
            reloadFunction = scope.reloadFunction
            backgroundLoadState = scope.backgroundLoadState
        }
    }
}

/**
 * Combine [this] container flow with another non-container flow. The result flow has
 * the same status (pending, error, or success) as origin container flow.
 */
public fun <T0, T1, R> Flow<Container<T0>>.containerCombineWith(
    flow: Flow<T1>,
    transform: suspend (T0, T1) -> R
): Flow<Container<R>> {
    return containerCombineWith(
        flows = listOf(flow),
        transform = { input ->
            @Suppress("UNCHECKED_CAST")
            transform(input[0] as T0, input[1] as T1)
        },
    )
}

/**
 * Combine [this] container flow with 2 other non-container flows. The result flow has
 * the same status (pending, error, or success) as origin container flow.
 */
public fun <T0, T1, T2, R> Flow<Container<T0>>.containerCombineWith(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    transform: suspend (T0, T1, T2) -> R
): Flow<Container<R>> {
    return containerCombineWith(
        flows = listOf(flow1, flow2),
        transform = { input ->
            @Suppress("UNCHECKED_CAST")
            transform(input[0] as T0, input[1] as T1, input[2] as T2)
        },
    )
}

/**
 * Combine [this] container flow with 3 other non-container flows. The result flow has
 * the same status (pending, error, or success) as origin container flow.
 */
public fun <T0, T1, T2, T3, R> Flow<Container<T0>>.containerCombineWith(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    transform: suspend (T0, T1, T2, T3) -> R
): Flow<Container<R>> {
    return containerCombineWith(
        flows = listOf(flow1, flow2, flow3),
        transform = { input ->
            @Suppress("UNCHECKED_CAST")
            transform(input[0] as T0, input[1] as T1, input[2] as T2, input[3] as T3)
        },
    )
}

/**
 * Combine [this] container flow with other non-container flows. The result flow has
 * the same status (pending, error, or success) as origin container flow.
 */
public fun <T, R> Flow<Container<T>>.containerCombineWith(
    flows: Iterable<Flow<*>>,
    transform: suspend CombineContainerFlowScope.(List<*>) -> R
): Flow<Container<R>> {
    val allFlows = listOf(this) + flows
    return combine(allFlows) { items ->
        val itemList = items.toList()
        @Suppress("UNCHECKED_CAST")
        val container = itemList[0] as Container<T>
        val scope = CombineContainerWithFlowScopeImpl(container)
        container
            .map { containerValue ->
                val values = listOf(containerValue) + itemList.subList(fromIndex = 1, toIndex = itemList.size)
                scope.transform(values)
            }
            .update {
                sourceType = scope.sourceType
                reloadFunction = scope.reloadFunction
                backgroundLoadState = scope.backgroundLoadState
            }
    }
}

private fun Array<Container<*>>.mergeReloadFunctions(): ReloadFunction {
    val reloadFunctions = map { container ->
        container.foldDefault(
            EmptyReloadFunction,
            onSuccess = { reloadFunction },
            onError = { reloadFunction },
        )
    }
    if (reloadFunctions.all { it == EmptyReloadFunction }) return EmptyReloadFunction
    return { backgroundLoad ->
        filterIsInstance<Container.Completed<*>>()
            .forEach { container ->
                container.reload(backgroundLoad)
            }
    }
}

public interface CombineContainerFlowScope {
    public var sourceType: SourceType
    public var backgroundLoadState: BackgroundLoadState
    public var reloadFunction: ReloadFunction
}

internal class CombineContainerFlowScopeImpl(
    containers: Array<Container<*>>,
) : CombineContainerFlowScope {

    override var sourceType: SourceType = (containers.firstOrNull {
        it is Container.Completed
    } as? Container.Completed)?.sourceType ?: UnknownSourceType

    override var backgroundLoadState: BackgroundLoadState =
        if (containers.any { it.backgroundLoadState == BackgroundLoadState.Loading }) {
            BackgroundLoadState.Loading
        } else {
            containers.firstOrNull { it.backgroundLoadState is BackgroundLoadState.Error }
                ?.backgroundLoadState
                ?: BackgroundLoadState.Idle
        }

    override var reloadFunction: ReloadFunction = containers.mergeReloadFunctions()

}

internal class CombineContainerWithFlowScopeImpl(
    container: Container<*>,
) : CombineContainerFlowScope {
    override var sourceType = (container as? Container.Completed)?.sourceType ?: UnknownSourceType
    override var backgroundLoadState = (container as? Container.Completed)?.backgroundLoadState ?: BackgroundLoadState.Loading
    override var reloadFunction = (container as? Container.Completed)?.reloadFunction ?: EmptyReloadFunction
}
