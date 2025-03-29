package io.tpersson.ufw.examples.common.commands

import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.annotations.AdminRequest

@AdminRequest(
    name = "Test Command #2",
    description =  "It does some other things, maybe."
)
public data class TestAdminCommand2(
    val input: String
) : Command<String>

public class TestAdminCommand2Handler : CommandHandler<TestAdminCommand2, String> {
    override suspend fun handle(command: TestAdminCommand2, context: Context): String {
        return command.input.reversed()
    }
}