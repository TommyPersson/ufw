package io.tpersson.ufw.mediator.middleware.cacheable

import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.CommandHandler
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.internal.ContextImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

internal class CacheableMiddlewareTest {

    private val handler = TestCommandHandler()
    private val middleware = CacheableMiddleware()

    @Test
    fun `handle - Can cache values based on 'cacheKey'`(): Unit = runBlocking {
        val res1 = handle(TestCommand(cacheKey = "1"))
        val res2 = handle(TestCommand(cacheKey = "1"))
        val res3 = handle(TestCommand(cacheKey = "2"))
        val res4 = handle(TestCommand(cacheKey = "2"))

        assertThat(res1).isSameAs(res2)
        assertThat(res3).isSameAs(res4)
    }

    @Test
    fun `handle - Cache functions according to configuration`(): Unit = runBlocking {
        val res1 = handle(TestCommand(cacheKey = "1"))
        delay(20)
        val res2 = handle(TestCommand(cacheKey = "1"))

        assertThat(res1).isNotSameAs(res2)
    }

    private suspend fun handle(command: TestCommand): Any? {
        val context = ContextImpl()

        return middleware.handle(command, context) { request, ctx ->
            handler.handle(request as TestCommand, ctx)
        }
    }

    data class TestCommand(
        override val cacheKey: String
    ) : Command<Result>,
        Cacheable<String> {

        override val cacheConfig = CacheConfig {
            expireAfterWrite = Duration.ofMillis(10)
        }
    }

    class TestCommandHandler : CommandHandler<TestCommand, Result> {
        override suspend fun handle(command: TestCommand, context: Context): Result {
            return Result("Hello, World")
        }
    }

    data class Result(val text: String)
}