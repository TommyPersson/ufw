package io.tpersson.ufw.mediator.middleware.timelimited

import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.internal.ContextImpl
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.fail

internal class TimeLimitedMiddlewareTest {

    @Test
    fun `handle - Command that doesn't exceed time limit`(): Unit = runBlocking {
        val command = TestCommand(delayTime = Duration.ofMillis(10))

        val context = ContextImpl()
        val handler = TestCommandHandler()
        val middleware = TimeLimitedMiddleware()

        middleware.handle(command, context) { request, ctx ->
            handler.handle(request as TestCommand, ctx)
        }
    }

    @Test
    fun `handle - Command that does exceed time limit`(): Unit = runBlocking {
        val command = TestCommand(delayTime = Duration.ofMillis(100))

        val context = ContextImpl()
        val handler = TestCommandHandler()
        val middleware = TimeLimitedMiddleware()

        try {
            middleware.handle(command, context) { request, ctx ->
                handler.handle(request as TestCommand, ctx)
            }
            fail("Did not rethrow TimeoutCancellationException")
        } catch (e: TimeoutCancellationException) {
        }
    }

    data class TestCommand(
        val delayTime: Duration
    ) : Command<Unit>,
        TimeLimited {

        override val timeout: Duration = Duration.ofMillis(50)

    }

    class TestCommandHandler : CommandHandler<TestCommand, Unit> {
        override suspend fun handle(command: TestCommand, context: Context) {
            delay(command.delayTime.toMillis())
        }
    }
}