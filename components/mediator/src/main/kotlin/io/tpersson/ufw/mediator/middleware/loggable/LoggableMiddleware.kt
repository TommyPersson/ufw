package io.tpersson.ufw.mediator.middleware.loggable

import io.tpersson.ufw.core.utils.LoggerCache
import io.tpersson.ufw.core.utils.measureTimedValue
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.ContextKey
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.middleware.StandardMiddlewarePriorities
import jakarta.inject.Inject
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.MDC

public class LoggableMiddleware @Inject constructor(
) : Middleware<Any, Any> {

    override val priority: Int = StandardMiddlewarePriorities.Loggable

    public object ContextKeys {
        public val Logger: ContextKey<Logger> = ContextKey("Loggable_Logger")
    }

    override suspend fun handle(
        request: Any,
        context: Context,
        next: suspend (request: Any, context: Context) -> Any
    ): Any {
        val logger = LoggerCache.get(request::class)

        context[ContextKeys.Logger] = logger

        MDC.put("requestType", request::class.simpleName)

        return withContext(MDCContext()) {
            val loggable = request as? Loggable

            try {
                val (result, duration) = measureTimedValue {
                    next(request, context)
                }

                loggable?.let { logger.info("Successful: ${it.logText}. [Duration = ${duration.toMillis()} ms]") }
                return@withContext result
            } catch (e: Exception) {
                loggable?.let { logger.error("Failed: ${it.logText}.", e) }
                throw e
            }
        }
    }
}

public val Context.logger: Logger
    get() = this[LoggableMiddleware.ContextKeys.Logger] ?: error("No Logger in Context!")