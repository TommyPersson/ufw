package io.tpersson.ufw.examples.common.commands

import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.annotations.AdminRequest

@AdminRequest(
    name = "Test Command #1",
    description =  "It returns a reversed copy of the given input"
)
public data class TestAdminCommand1(
    val input: String
) : Command<String>

public class TestAdminCommand1Handler : CommandHandler<TestAdminCommand1, String> {
    override suspend fun handle(command: TestAdminCommand1, context: Context): String {
        return command.input.reversed()
    }
}