package com.elveum.container.demo.feature.examples.reducer_container_flow

import com.elveum.container.Container
import com.elveum.container.successContainer
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepository @Inject constructor(
    private val faker: Faker
) {

    fun getRecentLogs(): Flow<Container<List<LogMessage>>> = flow {
        delay(2_000)
        val logs = ArrayDeque<LogMessage>()
        val levels = LogLevel.entries
        var index = 0
        while (true) {
            val level = levels[index % levels.size]
            val wordCount = (4..7).random()
            val message = faker.lorem().sentence(wordCount)
            val logMessage = LogMessage(
                id = index + 1L,
                rawMessage = message,
                level = level,
            )
            logs.add(logMessage)
            if (logs.size > 100) logs.removeFirst()
            emit(successContainer(logs.toList()))
            delay(700)
            index++
        }
    }

    enum class LogLevel {
        Info,
        Warning,
        Error,
    }

    data class LogMessage(
        val id: Long,
        val rawMessage: String,
        val level: LogLevel,
    ) {
        val logMessage = when (level) {
            LogLevel.Info -> "I: $rawMessage"
            LogLevel.Warning -> "W: $rawMessage"
            LogLevel.Error -> "E: $rawMessage"
        }
    }
}
