package io.tpersson.ufw.examples.guiceapp.commands

import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import jakarta.inject.Inject

public data class PerformGreetingCommand(
    val greetingKey: String
) : Command<Unit>

public class PerformGreetingCommandHandler @Inject constructor(
    private val keyValueStore: KeyValueStore
) : CommandHandler<PerformGreetingCommand, Unit> {
    override suspend fun handle(command: PerformGreetingCommand, context: Context) {
        val key = KeyValueStore.Key.of<String>(command.greetingKey)

        keyValueStore.put(key, "Hello, World!")

        val greeting = keyValueStore.get(key)?.value ?: "Missing Value"

        println(greeting)
    }
}