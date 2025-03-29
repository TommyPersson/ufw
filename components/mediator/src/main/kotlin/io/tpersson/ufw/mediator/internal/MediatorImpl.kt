package io.tpersson.ufw.mediator.internal

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.measureTimedValue
import io.tpersson.ufw.mediator.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST")
public class MediatorImpl(
    private val meterRegistry: MeterRegistry,
    handlers: Set<RequestHandler<*, *>>,
    middlewares: Set<Middleware<*, *>>
) : MediatorInternal {

    private val logger = createLogger()

    private val orderedMiddleware = middlewares.sortedBy { it.priority }

    init {
        val middlewareList = orderedMiddleware.joinToString("\n") {
            "    ${it::class.qualifiedName}; priority = ${it.priority},"
        }

        logger.info("Mediator initialized with middleware: \n[\n$middlewareList\n]")
    }

    private val middlewaresByRequest = ConcurrentHashMap<KClass<*>, List<Middleware<*, *>>>()
    private val handlersByRequest = handlers.associateBy { it.javaClass.kotlin.getRequestClass() }

    private val timers = handlersByRequest.keys.associateBy { it }.mapValues {
        Timer.builder("ufw.mediator.duration.seconds")
            .tag("requestType", it.key.simpleName.toString())
            .publishPercentiles(0.5, 0.75, 0.90, 0.99, 0.999)
            .register(meterRegistry)
    }

    public override val requestClasses: List<KClass<out Request<*>>> = handlersByRequest.keys.toList()

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = handlersByRequest[request::class] as? RequestHandler<TRequest, TResult>
            ?: error("No handler found for ${request::class.simpleName}")

        val selectedMiddlewares = getMiddlewaresFor(request).reversed()

        val context = ContextImpl()

        val initial: suspend (TRequest, Context) -> TResult = handler::handle

        val pipeline = selectedMiddlewares.foldRight(initial) { middleware, acc ->
            { request, context ->
                middleware.handle(request, context, acc)
            }
        }

        val (result, duration) = measureTimedValue {
            pipeline.invoke(request, context)
        }

        timers[request::class]?.record(duration)

        return result
    }

    private fun <TRequest : Request<TResult>, TResult> getMiddlewaresFor(request: TRequest): List<Middleware<TRequest, TResult>> {
        @Suppress("UNCHECKED_CAST")
        return middlewaresByRequest.getOrPut(request::class) {
            orderedMiddleware.filter { it.appliesTo(request::class) }
        } as List<Middleware<TRequest, TResult>>
    }

    private fun <TRequest : Request<TResult>, TResult> Middleware<*, *>.appliesTo(requestClass: KClass<TRequest>): Boolean {
        val rootRequestClass = this::class.supertypes[0].arguments[0].type!!.classifier as KClass<*>
        return requestClass.isSubclassOf(rootRequestClass)
    }

    private fun KClass<RequestHandler<*, *>>.getRequestClass(): KClass<out Request<*>> {
        return this.supertypes[0].arguments[0].type!!.classifier as KClass<out Request<*>>
    }
}
