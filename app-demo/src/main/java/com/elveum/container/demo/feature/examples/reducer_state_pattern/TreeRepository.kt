package com.elveum.container.demo.feature.examples.reducer_state_pattern

import com.elveum.container.ContainerFlow
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

class TreeRepository @Inject constructor(
    private val faker: Faker,
    private val random: Random,
) {

    private val deleteActions = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    // ContainerFlow<T> is an alias to Flow<Container<T>>
    fun getTree(): ContainerFlow<Tree> {
        return flow {
            emit(pendingContainer())
            delay(2000)
            val rootNode = generateTree()
            emit(successContainer(Tree(rootNode)))
            deleteActions.collect { id ->
                rootNode.delete(id)
                emit(successContainer(Tree(rootNode)))
            }
        }
    }

    fun deleteNode(nodeId: Long) {
        deleteActions.tryEmit(nodeId)
    }

    private fun generateTree(
        idGenerator: IdGenerator = IdGenerator(),
        level: Int = 4,
    ): Node {
        val childCount = if (level > 0) {
            random.nextInt(3, 6)
        } else {
            0
        }
        val node = Node(
            id = idGenerator.nextId(),
            title = faker.company().name(),
            description = faker.company().catchPhrase(),
            children = (1..childCount).map {
                generateTree(
                    idGenerator = idGenerator,
                    level = level - 1,
                )
            }.toMutableList()
        )
        return node
    }

    data class Node(
        val id: Long,
        val title: String,
        val description: String,
        val children: MutableList<Node>,
    ) {
        fun delete(id: Long) {
            val index = children.indexOfFirst { it.id == id }
            if (index != -1) {
                children.removeAt(index)
            } else {
                children.forEach { it.delete(id) }
            }
        }
    }

    class Tree(val rootNode: Node)

    class IdGenerator {
        private var idSeq: Long = 0
        fun nextId(): Long = ++idSeq
    }
}
