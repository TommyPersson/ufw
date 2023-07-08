package io.tpersson.ufw.examples.plainapp.commands

import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.middleware.transactional.Transactional
import io.tpersson.ufw.mediator.middleware.transactional.unitOfWork
import jakarta.inject.Inject

public data class PerformGreetingCommand(
    val target: String
) : Command<Unit>, Transactional

public class PerformGreetingCommandHandler @Inject constructor(
    private val keyValueStore: KeyValueStore
) : CommandHandler<PerformGreetingCommand, Unit> {
    private val numGreetingsPerformedKey = KeyValueStore.Key.of<Int>("num-greetings-performed")

    override suspend fun handle(command: PerformGreetingCommand, context: Context) {

        val current = keyValueStore.get(numGreetingsPerformedKey)?.value ?: 0

        keyValueStore.put(
            key = numGreetingsPerformedKey,
            value = current + 1,
            expectedVersion = current,
            unitOfWork = context.unitOfWork
        )

        context.unitOfWork.addPostCommitHook {
            val updated = keyValueStore.get(numGreetingsPerformedKey)?.value ?: 0
            println("$updated greetings have been performed")
        }

        println("Hello, ${command.target}!")
    }
}