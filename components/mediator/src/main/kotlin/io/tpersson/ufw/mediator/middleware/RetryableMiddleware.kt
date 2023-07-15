package io.tpersson.ufw.mediator.middleware

import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.ContextKey
import io.tpersson.ufw.mediator.Middleware
import jakarta.inject.Inject

/**
 * Marker interface for the [RetryableMiddleware].
 */
public interface Retryable {
    public val retryConfig: RetryConfig
}

public class RetryableMiddleware @Inject constructor(
) : Middleware<Retryable, Any> {

    override val priority: Int = StandardMiddlewarePriorities.Retryable

    private val retryRegistry = RetryRegistry.ofDefaults()

    public object ContextKeys {
        public val numAttempts: ContextKey<Int> = ContextKey("Retry_NumAttempts")
    }

    override suspend fun handle(
        request: Retryable,
        context: Context,
        next: suspend (request: Retryable, context: Context) -> Any
    ): Any {
        val retryId = "${request::class.java}-Retry"

        val retry = retryRegistry.retry(retryId, request.retryConfig)

        context[ContextKeys.numAttempts] = -1

        return retry.executeSuspendFunction {
            context[ContextKeys.numAttempts] = context[ContextKeys.numAttempts]!! + 1
            next(request, context)
        }
    }
}