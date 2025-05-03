package io.tpersson.ufw.examples.common.commands

import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.annotations.AdminRequest
import io.tpersson.ufw.mediator.annotations.AdminRequestParameter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

@AdminRequest(
    name = "Test Command #1",
    description =  "Used to test various parameter types"
)
public data class TestAdminCommand1(
    @AdminRequestParameter("A String", "Some help text", defaultValue = "Hello, World!")
    val aString: String,
    @AdminRequestParameter("An Optional String", "Can be left empty")
    val anOptionalString: String?,
    @AdminRequestParameter("An Int", "Some other help text", defaultValue = "123")
    val anInt: Int,
    @AdminRequestParameter("A Double")
    val aDouble: Double,
    @AdminRequestParameter("A LocalDate")
    val aLocalDate: LocalDate,
    @AdminRequestParameter("A LocalTime")
    val aLocalTime: LocalTime,
    @AdminRequestParameter("A LocalDateTime")
    val aLocalDateTime: LocalDateTime,
    @AdminRequestParameter("An OffsetDateTime")
    val anOffsetDateTime: OffsetDateTime,
) : Command<Any>

public class TestAdminCommand1Handler : CommandHandler<TestAdminCommand1, Any> {
    override suspend fun handle(command: TestAdminCommand1, context: Context): Any {
        return command
    }
}