package io.tpersson.ufw.mediator

public interface Mediator {
    public suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult

    public companion object {
        public fun create(
            handlers: Set<RequestHandler<*, *>>,
            middlewares: Set<Middleware<*, *>>
        ): MediatorImpl {
            return MediatorImpl(handlers, middlewares)
        }
    }
}
