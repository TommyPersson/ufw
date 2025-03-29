package io.tpersson.ufw.mediator

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tpersson.ufw.mediator.internal.MediatorImpl
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class IntegrationTest {

    companion object {
        private val invokedHandlers = mutableListOf<KClass<*>>()
        private val invokedMiddleware = mutableListOf<KClass<*>>()
        private val receivedContext = mutableMapOf<ContextKey<*>, Any>()
    }

    private val mediator = createMediator()

    @BeforeEach
    fun beforeEach() {
        invokedHandlers.clear()
        invokedMiddleware.clear()
        receivedContext.clear()
    }

    @Test
    fun `Basic - Invokes command handler`(): Unit = runBlocking {
        mediator.send(TestCommand1("Hello, World!"))

        assertThat(invokedHandlers).containsOnly(TestCommand1Handler::class)
    }

    @Test
    fun `Basic - Invokes query handler`(): Unit = runBlocking {
        val result = mediator.send(TestQuery1("Hello, World!"))

        assertThat(result).isEqualTo("Hello, World!")
        assertThat(invokedHandlers).containsOnly(TestQuery1Handler::class)
    }

    @Test
    fun `Middleware - Can apply middleware to any command`(): Unit = runBlocking {
        mediator.send(TestCommand1("Hello, World!"))

        assertThat(invokedMiddleware).contains(AnyCommandMiddleware::class)
    }

    @Test
    fun `Middleware - Can apply middleware to any query`(): Unit = runBlocking {
        mediator.send(TestQuery1("Hello, World!"))

        assertThat(invokedMiddleware).contains(AnyQueryMiddleware::class)
    }

    @Test
    fun `Middleware - Can apply middleware either request type`(): Unit = runBlocking {
        mediator.send(TestQuery1("Hello, World!"))

        assertThat(invokedMiddleware).contains(AnyRequestMiddleware::class)

        invokedMiddleware.clear()

        mediator.send(TestCommand1("Hello, World!"))

        assertThat(invokedMiddleware).contains(AnyRequestMiddleware::class)
    }

    @Test
    fun `Middleware - Are executed in order of priority (lower = earlier)`(): Unit = runBlocking {
        mediator.send(TestQuery1("Hello, World!"))

        assertThat(invokedMiddleware).isEqualTo(
            listOf(AnyRequestMiddleware::class, AnyQueryMiddleware::class)
        )
    }

    @Test
    fun `Middleware - Can apply to marker interfaces`(): Unit = runBlocking {
        mediator.send(TestCommandWithLogging("Hello, World!"))

        assertThat(invokedMiddleware).isEqualTo(
            listOf(LoggingRequestMiddleware::class, AnyRequestMiddleware::class, AnyCommandMiddleware::class)
        )
    }

    @Test
    fun `Middleware - Can provide context values to requests`(): Unit = runBlocking {
        mediator.send(TestCommand1("Hello, World!"))

        assertThat(receivedContext).containsEntry(AnyCommandMiddleware.ContextKeys.TestKey, "test-value")
    }

    private fun createMediator() = MediatorImpl(
        meterRegistry = SimpleMeterRegistry(),
        handlers = setOf(
            TestCommand1Handler(),
            TestQuery1Handler(),
            TestCommandWithLoggingHandler(),
        ),
        middlewares = setOf(
            AnyRequestMiddleware(),
            AnyCommandMiddleware(),
            AnyQueryMiddleware(),
            LoggingRequestMiddleware(),
        )
    )

    data class TestCommand1(val input: String) : Command<Unit>

    class TestCommand1Handler : CommandHandler<TestCommand1, Unit> {
        override suspend fun handle(command: TestCommand1, context: Context) {
            receivedContext.putAll(context.toMap())
            invokedHandlers.add(this::class)
        }
    }

    interface LoggingMarker

    data class TestCommandWithLogging(val input: String) : Command<Unit>, LoggingMarker

    class TestCommandWithLoggingHandler : CommandHandler<TestCommandWithLogging, Unit> {
        override suspend fun handle(command: TestCommandWithLogging, context: Context) {
            receivedContext.putAll(context.toMap())
            invokedHandlers.add(this::class)
        }
    }

    data class TestQuery1(val input: String) : Query<String>

    class TestQuery1Handler : QueryHandler<TestQuery1, String> {
        override suspend fun handle(query: TestQuery1, context: Context): String {
            receivedContext.putAll(context.toMap())
            invokedHandlers.add(this::class)
            return query.input
        }
    }

    class AnyCommandMiddleware : Middleware<Command<Any>, Any> {
        object ContextKeys {
            val TestKey = ContextKey<String>("test-key")
        }

        override val priority: Int
            get() = 1

        override suspend fun handle(
            request: Command<Any>,
            context: Context,
            next: suspend (request: Command<Any>, context: Context) -> Any
        ): Any {
            context[ContextKeys.TestKey] = "test-value"
            invokedMiddleware.add(this::class)
            return next(request, context)
        }
    }

    class AnyQueryMiddleware : Middleware<Query<Any>, Any> {
        override val priority: Int
            get() = 2

        override suspend fun handle(
            request: Query<Any>,
            context: Context,
            next: suspend (request: Query<Any>, context: Context) -> Any
        ): Any {
            invokedMiddleware.add(this::class)
            return next(request, context)
        }
    }

    class AnyRequestMiddleware : Middleware<Request<Any>, Any> {
        override val priority: Int
            get() = 3

        override suspend fun handle(
            request: Request<Any>,
            context: Context,
            next: suspend (request: Request<Any>, context: Context) -> Any
        ): Any {
            invokedMiddleware.add(this::class)
            return next(request, context)
        }
    }

    class LoggingRequestMiddleware : Middleware<LoggingMarker, Any> {
        override val priority: Int
            get() = 4

        override suspend fun handle(
            request: LoggingMarker,
            context: Context,
            next: suspend (request: LoggingMarker, context: Context) -> Any
        ): Any {
            invokedMiddleware.add(this::class)
            return next(request, context)
        }
    }
}
