package io.tpersson.ufw.mediator

public class MediatorComponent private constructor(
    public val mediator: Mediator
) {
    public companion object {
        public fun create(
            handlers: Set<RequestHandler<*, *>>,
            middlewares: Set<Middleware<*, *>>
        ): MediatorComponent {
            val mediator =  MediatorImpl(handlers, middlewares)

            return MediatorComponent(mediator)
        }
    }
}
