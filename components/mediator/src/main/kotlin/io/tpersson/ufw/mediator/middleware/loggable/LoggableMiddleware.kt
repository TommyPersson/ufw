package io.tpersson.ufw.mediator.middleware.loggable

import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.middleware.StandardMiddlewarePriorities
import jakarta.inject.Inject
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

public class LoggableMiddleware @Inject constructor(
) : Middleware<Loggable, Any> {

    override val priority: Int = StandardMiddlewarePriorities.Loggable

    private val loggers = ConcurrentHashMap<KClass<out Loggable>, Logger>()

    override suspend fun handle(
        request: Loggable,
        context: Context,
        next: suspend (request: Loggable, context: Context) -> Any
    ): Any {
        val logger = loggers.getOrPut(request::class) {
            LoggerFactory.getLogger(request::class.java)
        }

        MDC.put("requestType", request::class.simpleName)

        return withContext(MDCContext()) {
            try {
                val (result, duration) = measureTimedValue {
                    next(request, context)
                }

                logger.info("Successful: ${request.logText}. [Duration = ${duration.toString(DurationUnit.MILLISECONDS)}]")
                return@withContext result
            } catch (e: Exception) {
                logger.error("Failed: ${request.logText}.", e)
                throw e
            }
        }
    }
}