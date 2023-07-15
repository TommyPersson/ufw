package io.tpersson.ufw.mediator.middleware.timelimited

import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.middleware.StandardMiddlewarePriorities
import jakarta.inject.Inject
import kotlinx.coroutines.withTimeout

public class TimeLimitedMiddleware @Inject constructor(
) : Middleware<TimeLimited, Any> {

    override val priority: Int = StandardMiddlewarePriorities.TimeLimited

    override suspend fun handle(
        request: TimeLimited,
        context: Context,
        next: suspend (request: TimeLimited, context: Context) -> Any
    ): Any {
        val timeoutMillis = request.timeout.toMillis()

        return withTimeout(timeoutMillis) {
            next(request, context)
        }
    }
}