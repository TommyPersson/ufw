package io.tpersson.ufw.mediator

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.mediator.internal.ContextImpl
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

public class MediatorImpl(
    handlers: Set<RequestHandler<*, *>>,
    middlewares: Set<Middleware<*, *>>
) : Mediator {

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

        return pipeline.invoke(request, context)
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

    private fun KClass<RequestHandler<*, *>>.getRequestClass(): KClass<*> {
        return this.supertypes[0].arguments[0].type!!.classifier as KClass<*>
    }
}
