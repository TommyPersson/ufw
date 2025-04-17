package io.tpersson.ufw.mediator.internal

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.tpersson.ufw.core.utils.measureTimedValue
import io.tpersson.ufw.mediator.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST")
public class MediatorImpl(
    private val meterRegistry: MeterRegistry,
    private val handlerRegistry: MediatorRequestHandlerRegistry,
    private val middlewareRegistry: MediatorMiddlewareRegistry,
) : MediatorInternal {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var state = resetState()

    init {
        coroutineScope.launch {
            handlerRegistry.observe().collect {
                state = resetState()
            }

            middlewareRegistry.observe().collect {
                state = resetState()
            }
        }
    }

    public override val requestClasses: List<KClass<out Request<*>>> get() = state.handlersByRequest.keys.toList()

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = state.handlersByRequest[request::class] as? RequestHandler<TRequest, TResult>
            ?: error("No handler found for ${request::class.simpleName}")

        val selectedMiddlewares = state.getMiddlewaresFor(request).reversed()

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

        state.timers[request::class]?.record(duration)

        return result
    }

    private fun resetState() = State(
        requestHandlers = handlerRegistry.getAll(),
        middlewares = middlewareRegistry.getAll(),
        meterRegistry = meterRegistry
    )

    private inner class State(
        val requestHandlers: Set<RequestHandler<*, *>>,
        val middlewares: Set<Middleware<*, *>>,
        val meterRegistry: MeterRegistry,
    ) {
        val middlewaresByRequest = ConcurrentHashMap<KClass<*>, List<Middleware<*, *>>>()
        val handlersByRequest = requestHandlers.associateBy { it.javaClass.kotlin.getRequestClass() }
        val orderedMiddleware = middlewares.sortedBy { it.priority }

        val timers = handlersByRequest.keys.associateBy { it }.mapValues {
            Timer.builder("ufw.mediator.duration.seconds")
                .tag("requestType", it.key.simpleName.toString())
                .publishPercentiles(0.5, 0.75, 0.90, 0.99, 0.999)
                .register(meterRegistry)
        }

        fun <TRequest : Request<TResult>, TResult> getMiddlewaresFor(request: TRequest): List<Middleware<TRequest, TResult>> {
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
}
