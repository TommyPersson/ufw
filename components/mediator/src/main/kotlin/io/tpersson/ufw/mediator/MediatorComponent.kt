package io.tpersson.ufw.mediator

public class MediatorComponent private constructor(
    public val mediator: Mediator
) {
    public companion object {
        public fun create(
            handlers: List<RequestHandler<*, *>>,
            middlewares: List<Middleware<*, *>>
        ): MediatorComponent {
            val mediator =  MediatorImpl(handlers, middlewares)

            return MediatorComponent(mediator)
        }
    }
}