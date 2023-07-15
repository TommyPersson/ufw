package io.tpersson.ufw.mediator.middleware.retryable

import io.github.resilience4j.kotlin.retry.RetryConfig
import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.internal.ContextImpl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.fail

internal class RetryableMiddlewareTest {

    @Test
    fun `handle - Command that doesn't exceed retries`(): Unit = runBlocking {
        val command = TestCommand(3)

        val context = ContextImpl()
        val handler = TestCommandHandler()
        val middleware = RetryableMiddleware()

        middleware.handle(command, context) { request, ctx ->
            handler.handle(request as TestCommand, ctx)
        }
    }

    @Test
    fun `handle - Command that does exceed retries`(): Unit = runBlocking {
        val command = TestCommand(6)

        val context = ContextImpl()
        val handler = TestCommandHandler()
        val middleware = RetryableMiddleware()

        try {
            middleware.handle(command, context) { request, ctx ->
                handler.handle(request as TestCommand, ctx)
            }
            fail("Did not rethrow TestException")
        } catch (e: TestException) {
        }
    }

    data class TestCommand(
        val numTimesToFail: Int
    ) : Command<Unit>,
        Retryable {

        override val retryConfig = RetryConfig {
            waitDuration(Duration.ofMillis(1))
            maxAttempts(5)
        }
    }

    class TestCommandHandler : CommandHandler<TestCommand, Unit> {
        override suspend fun handle(command: TestCommand, context: Context) {
            val attempt = context[RetryableMiddleware.ContextKeys.numAttempts] ?: 0
            if (attempt < command.numTimesToFail) {
                throw TestException()
            }
        }
    }

    class TestException() : Exception()
}