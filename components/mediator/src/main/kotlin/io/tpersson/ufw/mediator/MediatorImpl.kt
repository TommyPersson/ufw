package io.tpersson.ufw.mediator

import io.tpersson.ufw.mediator.internal.ContextImpl
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

public class MediatorImpl(
    handlers: Set<RequestHandler<*, *>>,
    private val middlewares: Set<Middleware<*, *>>
) : Mediator {

    private val middlewaresByRequest = ConcurrentHashMap<KClass<*>, List<Middleware<*, *>>>()
    private val handlersByRequest = handlers.associateBy { it.javaClass.kotlin.getRequestClass() }

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = handlersByRequest[request::class] as? RequestHandler<TRequest, TResult>
            ?: error("No handler found for ${request::class.simpleName}")

        val selectedMiddlewares = getMiddlewaresFor(request)

        val context = ContextImpl()

        val initial: suspend (TRequest, Context) -> TResult = handler::handle

        val pipeline = selectedMiddlewares.foldRight(
            initial,
            { middleware, acc -> { request, context -> middleware.handle(request, context, acc) } },
        )

        return pipeline.invoke(request, context)
    }

    private fun <TRequest : Request<TResult>, TResult> getMiddlewaresFor(request: TRequest): List<Middleware<TRequest, TResult>> {
        @Suppress("UNCHECKED_CAST")
        return middlewaresByRequest.getOrPut(request::class) {
                middlewares
                    .filter { it.appliesTo(request::class) }
                    .sortedBy { it.priority }
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
