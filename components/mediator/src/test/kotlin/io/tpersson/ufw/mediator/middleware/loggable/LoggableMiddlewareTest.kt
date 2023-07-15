package io.tpersson.ufw.mediator.middleware.loggable

import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.internal.ContextImpl
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import kotlin.test.fail

internal class CacheableMiddlewareTest {

    private val handler = TestCommandHandler()
    private val middleware = LoggableMiddleware()

    @BeforeEach
    fun beforeEach() {
        handler.lastMDC = emptyMap()
    }

    @Test
    fun `handle - Returns the original result`(): Unit = runBlocking {
        val res = handle(TestCommand("Hello"))

        assertThat(res).isEqualTo(Result("Hello"))
    }

    @Test
    fun `handle - Rethrows original exception`(): Unit = runBlocking {
        try {
            handle(TestCommand("Hello", shouldFail = true))
            fail("Did not rethrow")
        } catch (e: TestException) {
        }
    }

    @Test
    fun `handle - Contains 'requestType' in MDC`(): Unit = runBlocking {
        handle(TestCommand("Hello"))

        assertThat(handler.lastMDC).containsEntry("requestType", "TestCommand")
    }

    private suspend fun handle(command: TestCommand): Any? {
        val context = ContextImpl()

        return middleware.handle(command, context) { request, ctx ->
            handler.handle(request as TestCommand, ctx)
        }
    }

    data class TestCommand(
        val someInput: String,
        val shouldFail: Boolean = false
    ) : Command<Result>,
        Loggable {

        override val logText by lazy { "Command with someInput = $someInput" }
    }

    class TestCommandHandler : CommandHandler<TestCommand, Result> {
        var lastMDC = emptyMap<String, String>()

        override suspend fun handle(command: TestCommand, context: Context): Result {
            lastMDC = MDC.getCopyOfContextMap()

            if (command.shouldFail) {
                throw TestException()
            }

            return Result(command.someInput)
        }
    }

    data class Result(val text: String)

    class TestException() : Exception()
}